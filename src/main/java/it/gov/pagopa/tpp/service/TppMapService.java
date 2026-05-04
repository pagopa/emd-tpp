package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service component for managing TPP entities in a distributed Redis cache via Redisson.
 *
 * <p>Uses {@link RMapReactive} (Redis Hash) for distributed caching shared across all pods,
 * and {@link RLockReactive} (distributed lock) to ensure that only <em>one</em> pod at a time
 * performs cache initialization or reset — the same pattern adopted by emd-citizen's
 * {@code BloomFilterInitializer}.</p>
 */
@Component
@Slf4j
public class TppMapService {

    private static final String LOCK_KEY = "emd:tpp:cache-reset-lock";

    private final TppRepository tppRepository;
    private final TokenSectionCryptService tokenSectionCryptService;
    private final RedissonReactiveClient redissonClient;
    private final RMapReactive<String, Tpp> tppMap;

    public TppMapService(TppRepository tppRepository,
                         TokenSectionCryptService tokenSectionCryptService,
                         RedissonReactiveClient redissonClient,
                         RMapReactive<String, Tpp> tppMap) {
        this.tppRepository = tppRepository;
        this.tokenSectionCryptService = tokenSectionCryptService;
        this.redissonClient = redissonClient;
        this.tppMap = tppMap;
    }

    /**
     * Populates the Redis cache with active TPP entities from the database at application startup.
     *
     * <p>Uses {@link Mono#usingWhen} to guarantee that the distributed lock is always released
     * (success, error, or cancellation) and that {@code block()} returns only <em>after</em>
     * the unlock command has completed on Redis — no fire-and-forget race.</p>
     *
     * <p>Uses {@code .block()} because {@code @PostConstruct} runs on a non-Reactor thread;
     * this guarantees the pod is NOT marked Ready until the cache is fully populated.</p>
     */
    @PostConstruct
    void populateMap() {
        Mono.usingWhen(
                acquireLock(),
                locked -> {
                    if (Boolean.FALSE.equals(locked)) {
                        log.info("[TPP-MAP][MAP-INITIALIZER] Another pod is initializing — skipping.");
                        return Mono.empty();
                    }
                    return tppMap.isExists()
                            .flatMap(exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    log.info("[TPP-MAP][MAP-INITIALIZER] Cache already populated by another pod — skipping.");
                                    return Mono.empty();
                                }
                                return doPopulate();
                            });
                },
                // asyncCleanup: called on complete, error AND cancel — properly chained, not fire-and-forget
                locked -> Boolean.TRUE.equals(locked) ? doReleaseLock() : Mono.empty()
        ).block(Duration.ofSeconds(120));
    }

    /**
     * Scheduled task that resets the Redis cache daily at 5 AM.
     *
     * <p>Uses {@link Mono#usingWhen} to guarantee that the distributed lock is always released
     * even if {@code performReset()} throws, and that {@code block()} returns only after the
     * unlock is confirmed by Redis.</p>
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void resetCache() {
        log.info("[TPP-MAP][CACHE-RESET] Starting Redis cache reset at 5 AM");
        try {
            Mono.usingWhen(
                    acquireLock(),
                    locked -> {
                        if (Boolean.FALSE.equals(locked)) {
                            log.info("[TPP-MAP][CACHE-RESET] Another pod is resetting — skipping.");
                            return Mono.empty();
                        }
                        return performReset();
                    },
                    locked -> Boolean.TRUE.equals(locked) ? doReleaseLock() : Mono.empty()
            ).block(Duration.ofSeconds(120));
        } catch (Exception e) {
            log.error("[TPP-MAP][CACHE-RESET] Reset failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Adds or updates a single TPP entity in the Redis cache with decrypted token section.
     *
     * @param tpp the TPP entity to cache
     * @return a Mono&lt;Boolean&gt; emitting {@code true} on success, {@code false} on decryption failure
     */
    public Mono<Boolean> addToMap(Tpp tpp) {
        String tppId = tpp.getTppId();
        return tokenSectionCryptService.keyDecrypt(tpp.getTokenSection(), tppId)
                .flatMap(decryptionResult ->
                        tppMap.put(tppId, tpp)
                                .doOnSuccess(old -> log.info("[TPP-MAP][ADD] Updated/Added TPP ID in cache: {}", tppId))
                                .thenReturn(true)
                )
                .onErrorResume(e -> {
                    log.error("[TPP-MAP][ADD] Decryption failed for TPP ID: {}", tppId, e);
                    return Mono.just(false);
                });
    }

    /**
     * Retrieves a TPP entity from the Redis cache by its identifier.
     *
     * @param tppId the TPP identifier to look up
     * @return a Mono containing the cached {@link Tpp}, or {@code Mono.empty()} if absent
     */
    public Mono<Tpp> getFromMap(String tppId) {
        return tppMap.get(tppId);
    }

    /**
     * Removes a TPP entity from the Redis cache by its identifier.
     *
     * @param tppId the TPP identifier to remove
     * @return a Mono&lt;Void&gt; that completes when the entry has been deleted
     */
    public Mono<Void> removeFromMap(String tppId) {
        return tppMap.remove(tppId)
                .doOnSuccess(removed -> log.info("[TPP-MAP][REMOVE] Removed TPP ID from cache: {}", tppId))
                .then();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Attempts to acquire the distributed lock with watchdog-based TTL management.
     *
     * <p>{@code waitTime = 0}: returns immediately with {@code false} if the lock is
     * already held by another pod (non-blocking).</p>
     *
     * <p>{@code leaseTime = -1}: enables Redisson's <em>watchdog</em> mechanism, which
     * automatically extends the lock TTL (every 10 s by default) while the holder is still
     * alive. This prevents the lock from expiring mid-reset even when Azure Key Vault
     * decrypt calls make {@code performReset()} take longer than a fixed TTL would allow.</p>
     */
    private Mono<Boolean> acquireLock() {
        return redissonClient.getLock(LOCK_KEY).tryLock(0, -1, TimeUnit.SECONDS);
    }

    /**
     * Releases the distributed lock via {@code forceUnlock()}.
     *
     * <p>{@code forceUnlock()} removes the lock key from Redis without checking thread
     * ownership — necessary because in reactive pipelines the thread that calls unlock
     * may differ from the one that acquired the lock (Netty event-loop scheduling).
     * It is safe here because {@code doReleaseLock()} is only ever invoked by the pod
     * that successfully acquired the lock (i.e., received {@code locked == true}).</p>
     *
     * <p>Returns {@code Mono<Void>} so it can be properly chained inside
     * {@link Mono#usingWhen} — the caller blocks until the Redis DEL has actually
     * completed, with no fire-and-forget race.</p>
     */
    private Mono<Void> doReleaseLock() {
        return redissonClient.getLock(LOCK_KEY).forceUnlock()
                .doOnSuccess(released -> log.info("[TPP-MAP] Lock released: {}", released))
                .doOnError(e -> log.error("[TPP-MAP] Failed to release lock: {}", e.getMessage()))
                .then();
    }

    private Mono<Void> doPopulate() {
        return buildSnapshotFromDb()
                .flatMap(snapshot -> {
                    if (snapshot.isEmpty()) {
                        log.info("[TPP-MAP][MAP-INITIALIZER] No active TPPs found in DB — cache stays empty.");
                        return Mono.empty();
                    }
                    return tppMap.putAll(snapshot)
                            .doOnSuccess(v -> log.info("[TPP-MAP][MAP-INITIALIZER] Population complete. Size: {}", snapshot.size()));
                });
    }

    private Mono<Void> performReset() {
        return buildSnapshotFromDb()
                .flatMap(snapshot -> tppMap.delete()
                        .then(snapshot.isEmpty()
                                ? Mono.<Void>empty()
                                : tppMap.putAll(snapshot))
                        .doOnSuccess(v -> log.info("[TPP-MAP][CACHE-RESET] Cache reset complete. New size: {}", snapshot.size())));
    }

    /**
     * Builds an in-memory snapshot of all active TPPs from MongoDB, decrypting each
     * TokenSection via Azure Key Vault.
     *
     * <p>Filters out TPPs with {@code state == null} or {@code state == false} and skips
     * any TPP whose decryption fails (logging the error). The returned snapshot can then
     * be written atomically to Redis via {@code putAll()}.</p>
     *
     * <p>⚠ Note: {@link TokenSectionCryptService#keyDecrypt} mutates the {@code TokenSection}
     * in-place; the {@code Tpp} instances stored in the snapshot therefore contain the
     * <em>decrypted</em> token section, ready to be served from cache.</p>
     */
    private Mono<Map<String, Tpp>> buildSnapshotFromDb() {
        Map<String, Tpp> snapshot = new ConcurrentHashMap<>();
        return tppRepository.findAll()
                .filter(tpp -> Boolean.TRUE.equals(tpp.getState()))
                .buffer(100)
                .flatMap(batch -> Flux.fromIterable(batch)
                        .flatMap(tpp -> tokenSectionCryptService.keyDecrypt(tpp.getTokenSection(), tpp.getTppId())
                                .doOnSuccess(ignored -> snapshot.put(tpp.getTppId(), tpp))
                                .onErrorResume(e -> {
                                    log.error("[TPP-MAP][SNAPSHOT] Decrypt failed for TPP ID: {}", tpp.getTppId(), e);
                                    return Mono.empty();
                                }))
                        .then())
                .then(Mono.fromSupplier(() -> snapshot));
    }

}