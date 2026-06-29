package it.gov.pagopa.tpp.integration;

import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.service.TppMapService;
import it.gov.pagopa.tpp.service.TokenSectionCryptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

import static it.gov.pagopa.tpp.utils.TestUtils.getMockTpp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration test verifying the Redis-backed TPP map (TppMapService) flow.
 *
 * <p>Uses real Redis and MongoDB via Testcontainers (configured in {@link BaseIT}).<br>
 * Azure Key Vault ({@link TokenSectionCryptService}) is mocked to avoid external
 * dependencies during CI runs.</p>
 *
 * <p>Scenarios covered:
 * <ol>
 *   <li>Service bean is correctly wired into the Spring context.</li>
 *   <li>{@code addToMap()} stores a TPP in Redis and {@code getFromMap()} retrieves it.</li>
 *   <li>{@code removeFromMap()} deletes a TPP from Redis.</li>
 *   <li>{@code resetCache()} loads only <em>active</em> TPPs from MongoDB into Redis.</li>
 *   <li>{@code resetCache()} clears stale entries and replaces them with fresh MongoDB data.</li>
 * </ol>
 * </p>
 */
public class TppMapServiceIT extends BaseIT {

    private static final Logger log = LoggerFactory.getLogger(TppMapServiceIT.class);

    private static final String COLLECTION_NAME = "tpp";
    private static final String TPP_ACTIVE_ID   = "tpp-active-it";
    private static final String TPP_INACTIVE_ID = "tpp-inactive-it";
    private static final String TPP_STALE_ID    = "tpp-stale-it";
    private static final String TPP_CURRENT_ID  = "tpp-current-it";

    @Autowired
    private TppMapService tppMapService;


    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    /**
     * Mocked to avoid real Azure Key Vault calls during integration tests.
     * Returns {@code Mono.just(true)} for all {@code keyDecrypt} invocations.
     */
    @MockitoBean
    private TokenSectionCryptService tokenSectionCryptService;

    @BeforeEach
    void setUp() {
        // Configure the Azure Key Vault mock so that token decryption always succeeds
        when(tokenSectionCryptService.keyDecrypt(any(), anyString()))
                .thenReturn(Mono.just(true));

        // Drop MongoDB collection to guarantee an empty state
        StepVerifier.create(
                mongoTemplate.dropCollection(COLLECTION_NAME)
                        .onErrorResume(e -> Mono.empty())
        ).verifyComplete();

        // Reset Redis map by running resetCache() against an empty MongoDB
        // → performReset() will delete the map and rebuild it from an empty Flux
        tppMapService.resetCache();
    }

    // -------------------------------------------------------------------------
    // 1. Service bean wiring
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link TppMapService} is correctly instantiated and injected
     * by the Spring context (i.e., the Redisson and MongoDB wiring is valid).
     */
    @Test
    void testTppMapServiceBeanLoaded() {
        log.info("=== EXECUTING testTppMapServiceBeanLoaded ===");

        StepVerifier.create(Mono.fromSupplier(() -> tppMapService))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    // -------------------------------------------------------------------------
    // 2. addToMap + getFromMap round-trip
    // -------------------------------------------------------------------------

    /**
     * Verifies that adding a TPP to the Redis map and immediately reading it back
     * returns the correct entry.
     *
     * <ul>
     *   <li>Act: {@code addToMap()} on an active TPP.</li>
     *   <li>Assert: {@code getFromMap()} returns a TPP with the expected ID.</li>
     * </ul>
     */
    @Test
    void testAddAndGetFromMap() {
        log.info("=== EXECUTING testAddAndGetFromMap ===");

        Tpp tpp = getMockTpp(TPP_ACTIVE_ID, true);

        StepVerifier.create(tppMapService.addToMap(tpp))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(tppMapService.getFromMap(TPP_ACTIVE_ID))
                .assertNext(result -> {
                    log.info("Retrieved TPP: {}", result.getTppId());
                    assertEquals(TPP_ACTIVE_ID, result.getTppId());
                })
                .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    // -------------------------------------------------------------------------
    // 3. removeFromMap
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@code removeFromMap()} deletes the entry from Redis so that
     * a subsequent {@code getFromMap()} returns an empty Mono.
     *
     * <ul>
     *   <li>Arrange: add a TPP to the map.</li>
     *   <li>Act: remove it.</li>
     *   <li>Assert: {@code getFromMap()} completes without emitting any item.</li>
     * </ul>
     */
    @Test
    void testRemoveFromMap() {
        log.info("=== EXECUTING testRemoveFromMap ===");

        Tpp tpp = getMockTpp(TPP_ACTIVE_ID, true);
        tppMapService.addToMap(tpp).block();

        StepVerifier.create(tppMapService.removeFromMap(TPP_ACTIVE_ID))
                .verifyComplete();

        StepVerifier.create(tppMapService.getFromMap(TPP_ACTIVE_ID))
                .verifyComplete(); // must be absent

        log.info("=== TEST COMPLETED ===");
    }

    // -------------------------------------------------------------------------
    // 4. resetCache — only active TPPs are loaded
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@code resetCache()} loads <em>only active</em> TPPs from MongoDB.
     *
     * <ul>
     *   <li>Arrange: persist one active TPP and one inactive TPP in MongoDB.</li>
     *   <li>Act: call {@code resetCache()}.</li>
     *   <li>Assert:
     *     <ul>
     *       <li>The active TPP is present in the Redis map.</li>
     *       <li>The inactive TPP is absent from the Redis map.</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    @Test
    void testResetCachePopulatesOnlyActiveTpps() {
        log.info("=== EXECUTING testResetCachePopulatesOnlyActiveTpps ===");

        Tpp activeTpp   = getMockTpp(TPP_ACTIVE_ID,   true);
        Tpp inactiveTpp = getMockTpp(TPP_INACTIVE_ID, false);

        StepVerifier.create(
                mongoTemplate.save(activeTpp,   COLLECTION_NAME)
                        .then(mongoTemplate.save(inactiveTpp, COLLECTION_NAME))
        ).expectNextCount(1).verifyComplete();

        tppMapService.resetCache();

        // Active TPP must be in Redis
        StepVerifier.create(tppMapService.getFromMap(TPP_ACTIVE_ID))
                .assertNext(t -> {
                    log.info("Active TPP found: {}", t.getTppId());
                    assertEquals(TPP_ACTIVE_ID, t.getTppId());
                })
                .verifyComplete();

        // Inactive TPP must NOT be in Redis
        StepVerifier.create(tppMapService.getFromMap(TPP_INACTIVE_ID))
                .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    // -------------------------------------------------------------------------
    // 5. resetCache — stale entries are cleared
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@code resetCache()} removes stale Redis entries that are no
     * longer present (or active) in MongoDB, replacing them with fresh data.
     *
     * <ul>
     *   <li>Arrange: manually add a stale TPP directly to the Redis map;
     *       persist a different, current TPP in MongoDB.</li>
     *   <li>Act: call {@code resetCache()}.</li>
     *   <li>Assert:
     *     <ul>
     *       <li>The stale entry is gone from Redis.</li>
     *       <li>The current MongoDB entry is now present in Redis.</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    @Test
    void testResetCacheClearsStaleEntries() {
        log.info("=== EXECUTING testResetCacheClearsStaleEntries ===");

        // Arrange: stale entry directly in Redis (not in MongoDB)
        Tpp staleTpp = getMockTpp(TPP_STALE_ID, true);
        tppMapService.addToMap(staleTpp).block();

        // Arrange: current entry in MongoDB
        Tpp currentTpp = getMockTpp(TPP_CURRENT_ID, true);
        StepVerifier.create(mongoTemplate.save(currentTpp, COLLECTION_NAME))
                .expectNextCount(1).verifyComplete();

        tppMapService.resetCache();

        // Stale entry must be gone
        StepVerifier.create(tppMapService.getFromMap(TPP_STALE_ID))
                .verifyComplete();

        // Current entry must be present
        StepVerifier.create(tppMapService.getFromMap(TPP_CURRENT_ID))
                .assertNext(t -> {
                    log.info("Current TPP found: {}", t.getTppId());
                    assertEquals(TPP_CURRENT_ID, t.getTppId());
                })
                .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }
}

