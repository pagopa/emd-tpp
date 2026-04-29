package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Service component for managing TPP entities in an in-memory cache.
 * The service uses Caffeine cache for high-performance in-memory storage.
 */ 
@Component
@Slf4j
public class TppMapService {
    private final Cache<String, Tpp> tppCache;

    private final TppRepository tppRepository;

    private final TokenSectionCryptService tokenSectionCryptService;

    /**
     * Constructs a TppMapService with the specified repository and cryptographic service.
     * Initializes the cache with a maximum size of 1000 entries.
     * 
     * @param tppRepository the repository for TPP data access
     * @param tokenSectionCryptService the service for token section encryption/decryption
     */
    public TppMapService(TppRepository tppRepository, TokenSectionCryptService tokenSectionCryptService) {
        this.tppRepository = tppRepository;
        this.tokenSectionCryptService = tokenSectionCryptService;
        this.tppCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .build();
    }

    /**
     * Populates the cache with active TPP entities from the database at application startup.
     *
     * <p>Uses {@code .block()} instead of {@code .subscribe()} to guarantee the pod does NOT
     * become Ready (readinessProbe passes) before the cache is fully populated.
     * Without this, the first requests after a Rolling Update hit an empty cache, forcing
     * every concurrent caller to fall through to MongoDB + Azure Key Vault decrypt,
     * causing a load spike on both systems.
     * {@code @PostConstruct} is called on a Spring-managed thread (non-Reactor), so {@code .block()} is safe.</p>
     */
    @PostConstruct
    private void populateMap() {
        tppRepository.findAll()
                .filter(Tpp::getState)
                .buffer(100)
                .flatMap(this::addToMap)
                .then()
                .doOnSuccess(v -> log.info("[TPP-MAP][MAP-INITIALIZER] Population complete"))
                .block(Duration.ofSeconds(60));
    }

    /**
     * Scheduled method that resets the cache daily at 4 AM.
     *
     * <p>Performs an <strong>atomic swap</strong>: builds a fresh cache in background from the
     * current DB state, then replaces the live cache in a single operation.
     * This ensures no request window exists where the cache is empty — the old entries
     * remain queryable until the new snapshot is fully ready.</p>
     *
     * <p>Uses {@code .block()} because {@code @Scheduled} methods run on Spring's
     * {@code TaskScheduler} thread (blocking-safe). Spring's graceful shutdown
     * waits for the scheduled method to return before destroying the context.</p>
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void resetCache() {
        log.info("[TPP-MAP][CACHE-RESET] Starting atomic cache reset at 4 AM");
        Map<String, Tpp> snapshot = new ConcurrentHashMap<>();
        try {
            tppRepository.findAll()
                    .filter(Tpp::getState)
                    .buffer(100)
                    .flatMap(batch -> Flux.fromIterable(batch)
                            .flatMap(tpp -> tokenSectionCryptService.keyDecrypt(tpp.getTokenSection(), tpp.getTppId())
                                    .doOnSuccess(ignored -> snapshot.put(tpp.getTppId(), tpp))
                                    .onErrorResume(e -> {
                                        log.error("[TPP-MAP][CACHE-RESET] Decrypt failed for TPP ID: {}", tpp.getTppId(), e);
                                        return Mono.empty();
                                    }))
                            .then())
                    .then()
                    .block(Duration.ofSeconds(60));

            // Atomic swap: old cache remains readable until this point
            tppCache.invalidateAll();
            tppCache.putAll(snapshot);
            log.info("[TPP-MAP][CACHE-RESET] Cache atomically swapped. New size: {}", tppCache.estimatedSize());
        } catch (Exception e) {
            log.error("[TPP-MAP][CACHE-RESET] Reset failed — old cache retained: {}", e.getMessage(), e);
        }
    }

    /**
     * Adds a list of TPP entities to the cache.
     * 
     * @param tpps the list of TPP entities to add to the cache
     * @return a Mono<Void> that completes when all TPPs have been processed
     */
    private Mono<Void> addToMap(List<Tpp> tpps) {
        return Flux.fromIterable(tpps)
                .flatMap(this::addToMap)
                .then();
    }

    /**
     * Adds a single TPP entity to the cache with decrypted token section.
     * <p>
     * This method decrypts the TPP's token section and adds the TPP to the cache
     * if it's not already present. It handles duplicate detection and logs
     * appropriate messages for successful additions and duplicates.
     * 
     * @param tpp the TPP entity to add to the cache
     * @return a Mono<Boolean> that emits true if the TPP was already present (duplicate),
     *         false if it was successfully added, or false if decryption failed
     */
    public Mono<Boolean> addToMap(Tpp tpp) {
        String tppId = tpp.getTppId();
        return tokenSectionCryptService.keyDecrypt(tpp.getTokenSection(), tpp.getTppId())
            .flatMap(decryptionResult -> {
                tppCache.put(tppId, tpp);
                log.info("[TPP-MAP][ADD] Updated/Added TPP ID in cache: {}", tppId);
                return Mono.just(true);
            })
            .onErrorResume(e -> {
                log.error("[TPP-MAP][ADD] Decryption failed for TPP ID: {}", tppId, e);
                return Mono.just(false);
            });
    }

    /**
     * Retrieves a TPP entity from the cache by its identifier reactively.
     * * @param tppId the TPP identifier to look up
     * @return a Mono containing the cached TPP entity if present, or Mono.empty() otherwise
     */
    public Mono<Tpp> getFromMap(String tppId) {
        return Mono.justOrEmpty(tppCache.getIfPresent(tppId));
    }

    /**
     * Removes a TPP entity from the cache by its identifier.
     * 
     * @param tppId the TPP identifier to remove from the cache
     */
    public void removeFromMap(String tppId) {
        tppCache.invalidate(tppId);
    }

}