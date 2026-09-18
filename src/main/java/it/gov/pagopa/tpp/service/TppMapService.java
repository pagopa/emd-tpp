package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final RMapReactive<String, String> entityIdToTppIdMap;
    private final Duration pollInterval;

    @Autowired
    public TppMapService(TppRepository tppRepository,
                         TokenSectionCryptService tokenSectionCryptService,
                         RedissonReactiveClient redissonClient,
                         RMapReactive<String, Tpp> tppMap,
                         RMapReactive<String, String> entityIdToTppIdMap) {
        this(tppRepository, tokenSectionCryptService, redissonClient, tppMap, entityIdToTppIdMap, Duration.ofSeconds(5));
    }

    /** Package-private constructor — used by unit tests to inject a short poll interval. */
    TppMapService(TppRepository tppRepository,
                  TokenSectionCryptService tokenSectionCryptService,
                  RedissonReactiveClient redissonClient,
                  RMapReactive<String, Tpp> tppMap,
                  RMapReactive<String, String> entityIdToTppIdMap,
                  Duration pollInterval) {
        this.tppRepository = tppRepository;
        this.tokenSectionCryptService = tokenSectionCryptService;
        this.redissonClient = redissonClient;
        this.tppMap = tppMap;
        this.pollInterval = pollInterval;
        this.entityIdToTppIdMap = entityIdToTppIdMap;
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
                        log.info("[TPP-MAP][MAP-INITIALIZER] Another pod is initializing — waiting for cache to be ready...");
                        return waitForCachePopulated();
                    }
                    // Step 1: check whether both cache maps already exist before starting initialization.
                    return Mono.zip(
                                tppMap.isExists(),
                                entityIdToTppIdMap.isExists())
                            .flatMap(tuple -> {
                                boolean tppMapExists = Boolean.TRUE.equals(tuple.getT1());
                                boolean entityIdIndexExists = Boolean.TRUE.equals(tuple.getT2());

                                // Step 2: skip initialization only when both cache structures already exist.
                                if (tppMapExists && entityIdIndexExists) {
                                    log.info("[TPP-MAP][MAP-INITIALIZER] Cache and entityId index already populated by another pod — skipping.");
                                    return Mono.empty();
                                }

                                // Step 3: initialize both cache structures from the database.
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
     * @param tpp the TPP entity to cache (tokenSection must be ENCRYPTED — this method decrypts it)
     * @return a Mono&lt;Boolean&gt; emitting {@code true} on success, {@code false} on decryption failure
     */
    public Mono<Boolean> addToMap(Tpp tpp) {
        String tppId = tpp.getTppId();
        String entityId = tpp.getEntityId();
        
        return tokenSectionCryptService.keyDecrypt(tpp.getTokenSection(), tppId)
                .flatMap(decryptionResult ->
                        tppMap.put(tppId, tpp)
                                .then(entityIdToTppIdMap.put(entityId, tppId))
                                .doOnSuccess(old -> log.info("[TPP-MAP][ADD] Updated/Added TPP ID {} and EntityID {} in cache", tppId, entityId))
                                .thenReturn(true)
                )
                .onErrorResume(e -> {
                    log.error("[TPP-MAP][ADD] Decryption failed for TPP ID: {}, entityId: {}", tppId, entityId, e);
                    return Mono.just(false);
                });
    }

    /**
     * Stores an already-decrypted TPP entity directly into the Redis cache, skipping the
     * decryption step. Use this when the caller has already decrypted the {@code TokenSection}
     * (e.g., after an explicit {@code keyDecrypt} call) to avoid double-decryption.
     *
     * @param tpp the TPP entity to cache (tokenSection must already be DECRYPTED)
     * @return a Mono&lt;Boolean&gt; emitting {@code true} on success, {@code false} on cache error
     */
    public Mono<Boolean> addDecryptedToMap(Tpp tpp) {
        String tppId = tpp.getTppId();
        String entityId = tpp.getEntityId();

        return tppMap.put(tppId, tpp)
                .then(entityIdToTppIdMap.put(entityId, tppId))
                .doOnSuccess(old -> log.info("[TPP-MAP][ADD] Updated/Added decryptedTPP ID {} and EntityID {} in cache", tppId, entityId))
                .thenReturn(true)
                .onErrorResume(e -> {
                    log.error("[TPP-MAP][ADD] Failed to cache already-decrypted TPP ID: {}, entityId={}", tppId, entityId, e);
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
     * Retrieves a TPP entity from the Redis cache by its entityId.
     *
     * @param entityId the TPP identifier to look up
     * @return a Mono containing the cached {@link Tpp}, or {@code Mono.empty()} if absent
     */
    public Mono<Tpp> getFromMapByEntityId(String entityId) {
        return entityIdToTppIdMap.get(entityId)
                .flatMap(tppId -> tppMap.get(tppId));
    }

    /**
     * Removes a TPP entity from the Redis cache by its identifier.
     *
     * @param tpp the TPP to remove
     * @return a Mono&lt;Void&gt; that completes when the entry has been deleted
     */
    public Mono<Void> removeFromMap(Tpp tpp) {
        String tppId = tpp.getTppId();
        String entityId = tpp.getEntityId();

        return tppMap.remove(tppId)
                .then(entityIdToTppIdMap.remove(entityId))
                .doOnSuccess(removed -> log.info("[TPP-MAP][REMOVE] Removed TPP ID: {} and EntityID: {}", tppId, entityId))
                .then();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Polls Redis every 5 seconds until the TPP cache key exists, meaning another pod has
     * finished populating it. Called by {@link #populateMap()} when the distributed lock
     * could not be acquired (another pod is initializing).
     *
     * <p>The outer {@code block(Duration.ofSeconds(120))} in {@link #populateMap()} acts
     * as the overall timeout, so this pod will NOT be marked Ready until the cache is
     * available — preventing it from serving stale/empty data during rolling updates.</p>
     */
    private Mono<Void> waitForCachePopulated() {
    return Flux.interval(pollInterval)
            // Step 1: wait until both Redis cache structures are available.
            .flatMap(tick -> Mono.zip(
                    tppMap.isExists(),
                    entityIdToTppIdMap.isExists()
            ))
            // Step 2: proceed only when both cache structures exist.
            .filter(tuple ->
                    Boolean.TRUE.equals(tuple.getT1()) &&
                    Boolean.TRUE.equals(tuple.getT2())
            )
            // Step 3: stop polling as soon as the cache is ready.
            .next()
            .doOnSuccess(v ->log.info("[TPP-MAP][MAP-INITIALIZER] Cache and entityId index are now ready — proceeding."))
            .then();
}

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
                    Map<String, String> entityIdSnapshot = snapshot.values().stream()
                            .collect(Collectors.toMap(tpp -> tpp.getEntityId(), tpp -> tpp.getTppId()));
                    return tppMap.putAll(snapshot)
                            .then(entityIdToTppIdMap.putAll(entityIdSnapshot))
                            .doOnSuccess(v -> log.info("[TPP-MAP][MAP-INITIALIZER] Population complete. Size: {}", snapshot.size()));
                });
    }

    private Mono<Void> performReset() {
        // Step 1: read the current keys BEFORE making any changes (source of truth for eviction).
        return Mono.zip(tppMap.readAllKeySet(),
                        entityIdToTppIdMap.readAllKeySet(),
                        buildSnapshotFromDb()).
                        flatMap(tuple -> {
                            Set<String> currentTppIds = tuple.getT1();
                            Set<String> currentEntityIds = tuple.getT2();
                            Map<String, Tpp> newSnapshot = tuple.getT3();

                            // Step 2: build the new entityId -> tppId index from the database snapshot.
                            Map<String, String> newEntityIdSnapshot = newSnapshot.values().stream()
                                    .collect(Collectors.toMap(tpp -> tpp.getEntityId(), tpp -> tpp.getTppId()));

                            // Step 3: upsert TPPs into the main cache without creating an empty-cache window.
                            Mono<Void> upsertTppCache = newSnapshot.isEmpty()
                                    ? Mono.empty()
                                    : tppMap.putAll(newSnapshot);

                            // Step 4: upsert the entityId -> tppId index consistently with the new TPP snapshot.
                            Mono<Void> upsertEntityIdIndex = newEntityIdSnapshot.isEmpty()
                                    ? Mono.empty()
                                    : entityIdToTppIdMap.putAll(newEntityIdSnapshot);

                            // Step 5: identify TPP IDs that are no longer present in the new snapshot.
                            List<String> staleTppIds = currentTppIds.stream()
                                    .filter(tppId -> !newSnapshot.containsKey(tppId))
                                    .collect(Collectors.toList());

                            // Step 6: identify entityIds that are no longer present in the new index.
                            List<String> staleEntityIds = currentEntityIds.stream()
                                    .filter(entityId -> !newEntityIdSnapshot.containsKey(entityId))
                                    .collect(Collectors.toList());

                            // Step 7: remove TPPs that are no longer present in the new snapshot.
                            Mono<Void> evictTppCache = staleTppIds.isEmpty()
                                    ? Mono.empty()
                                    : tppMap.fastRemove(staleTppIds.toArray(new String[0])).then();

                            // Step 8: remove entityId mappings that are no longer present in the new index.
                            Mono<Void> evictEntityIdIndex = staleEntityIds.isEmpty()
                                    ? Mono.empty()
                                    : entityIdToTppIdMap.fastRemove(staleEntityIds.toArray(new String[0]))
                                    .then();

                            // Step 9: update both cache structures before removing stale entries.
                            return upsertTppCache
                                    .then(upsertEntityIdIndex)
                                    .then(evictTppCache)
                                    .then(evictEntityIdIndex)
                                    .doOnSuccess(v -> log.info(
                                            "[TPP-MAP][CACHE-RESET] Cache reset complete. " +
                                            "TPPs: {}, entityId index: {}, evicted TPPs: {}, evicted entityIds: {}",
                                            newSnapshot.size(), newEntityIdSnapshot.size(), staleTppIds.size(),staleEntityIds.size()));
                        });
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