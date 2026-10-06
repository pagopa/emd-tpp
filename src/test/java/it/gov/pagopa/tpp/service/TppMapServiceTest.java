package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.model.TokenSection;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLockReactive;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static it.gov.pagopa.tpp.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith({SpringExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
class TppMapServiceTest {

    private static final String LOCK_KEY = "emd:tpp:cache-reset-lock";

    private RMapReactive<String, Tpp> tppMap;

    private RMapReactive<String, String> entityIdToTppIdMap;

    @Mock
    private RLockReactive lock;

    @MockitoBean
    private RedissonReactiveClient redissonClient;

    @MockitoBean
    private TppRepository tppRepository;

    @MockitoBean
    private TokenSectionCryptService tokenSectionCryptService;

    private TppMapService tppMapService;

    private Tpp tpp;

    @BeforeEach
    void setUp() {
        tpp = getMockTpp();
        tppMap = mock(RMapReactive.class);
        entityIdToTppIdMap = mock(RMapReactive.class);

        MockitoAnnotations.openMocks(this);

        // Lock setup (same pattern as BloomFilterInitializerTest in emd-citizen)
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(0, -1, TimeUnit.SECONDS)).thenReturn(Mono.just(true));
        when(lock.forceUnlock()).thenReturn(Mono.just(true));

        // Map setup — isExists() defaults to false (fresh cache) for populateMap tests
        when(tppMap.isExists()).thenReturn(Mono.just(false));
        when(tppMap.put(anyString(), any(Tpp.class))).thenReturn(Mono.empty());
        when(tppMap.get(anyString())).thenReturn(Mono.empty());
        when(tppMap.remove(anyString())).thenReturn(Mono.empty());
        when(tppMap.delete()).thenReturn(Mono.just(true));
        when(tppMap.putAll(anyMap())).thenReturn(Mono.empty());
        when(tppMap.readAllKeySet()).thenReturn(Mono.just(new HashSet<>()));
        when(tppMap.fastRemove(any(String[].class))).thenReturn(Mono.just(0L));

        // Secondary entityId -> tppId index setup
        when(entityIdToTppIdMap.isExists()).thenReturn(Mono.just(false));
        when(entityIdToTppIdMap.put(anyString(), anyString())).thenReturn(Mono.empty());
        when(entityIdToTppIdMap.get(anyString())).thenReturn(Mono.empty());
        when(entityIdToTppIdMap.remove(anyString())).thenReturn(Mono.empty());
        when(entityIdToTppIdMap.delete()).thenReturn(Mono.just(true));
        when(entityIdToTppIdMap.putAll(anyMap())).thenReturn(Mono.empty());
        when(entityIdToTppIdMap.readAllKeySet()).thenReturn(Mono.just(new HashSet<>()));
        when(entityIdToTppIdMap.fastRemove(any(String[].class))).thenReturn(Mono.just(0L));

        // Repository and crypto
        when(tppRepository.findAll()).thenReturn(Flux.just(tpp));
        when(tokenSectionCryptService.keyDecrypt(any(TokenSection.class), anyString()))
                .thenReturn(Mono.just(true));

        tppMapService = new TppMapService(tppRepository, tokenSectionCryptService, redissonClient, tppMap, entityIdToTppIdMap, Duration.ofMillis(100));
        tppMapService.resetCache();
    }

    // -------------------------------------------------------------------------
    // addToMap
    // -------------------------------------------------------------------------

    @Test
    void testGetFromCache() {
        when(tppMap.get(tpp.getTppId())).thenReturn(Mono.just(tpp));

        tppMapService.addToMap(tpp).block();

        StepVerifier.create(tppMapService.getFromMap(tpp.getTppId()))
                .expectNext(tpp)
                .verifyComplete();
    }

    /**
     * When Azure Key Vault decrypt throws, addToMap must swallow the error and return false.
     */
    @Test
    void addToMap_decryptionFails_returnsFalse() {
        when(tokenSectionCryptService.keyDecrypt(any(TokenSection.class), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Key Vault unavailable")));

        StepVerifier.create(tppMapService.addToMap(tpp))
                .expectNext(false)
                .verifyComplete();

        // tppMap.put() must NOT have been called during this invocation
        verify(tppMap, never()).put(eq(tpp.getTppId()), eq(tpp));
    }

    // -------------------------------------------------------------------------
    // addDecryptedToMap
    // -------------------------------------------------------------------------

    /**
     * addDecryptedToMap must store the tpp directly (no keyDecrypt call) and return true.
     */
    @Test
    void addDecryptedToMap_ok_returnsTrueAndStoresInCache() {
        clearInvocations(tppMap, tokenSectionCryptService);

        StepVerifier.create(tppMapService.addDecryptedToMap(tpp))
                .expectNext(true)
                .verifyComplete();

        verify(tppMap).put(tpp.getTppId(), tpp);
        // keyDecrypt must NOT be called — tpp is already decrypted
        verify(tokenSectionCryptService, never()).keyDecrypt(any(), any());
    }

    /**
     * When the Redis put fails, addDecryptedToMap must swallow the error and return false.
     */
    @Test
    void addDecryptedToMap_cacheFails_returnsFalse() {
        when(tppMap.put(anyString(), any(Tpp.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis unavailable")));

        StepVerifier.create(tppMapService.addDecryptedToMap(tpp))
                .expectNext(false)
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // resetCache — stale key eviction
    // -------------------------------------------------------------------------

    /**
     * When Redis contains a key that is no longer active in MongoDB,
     * performReset must call fastRemove to evict it.
     */
    @Test
    void resetCache_staleKeyInRedis_isEvicted() {
        // Step 1: simulate a stale TPP key and its corresponding entityId index entry.
        when(tppMap.readAllKeySet())
                .thenReturn(Mono.just(new HashSet<>(Set.of("staleKey"))));

        when(entityIdToTppIdMap.readAllKeySet())
                .thenReturn(Mono.just(new HashSet<>(Set.of("staleEntityId"))));

        clearInvocations(tppMap, entityIdToTppIdMap);

        // Step 2: execute the cache reset.
        tppMapService.resetCache();

        // Step 3: verify that stale TPP data is evicted from the main cache.
        verify(tppMap).fastRemove(any(String[].class));

        // Step 4: verify that stale entityId index data is evicted from the secondary cache.
        verify(entityIdToTppIdMap).fastRemove(any(String[].class));

        // Step 5: verify that the active TPP is still upserted into the main cache.
        verify(tppMap).putAll(argThat(map ->
                map.size() == 1 && map.containsKey(tpp.getTppId())
        ));

        // Step 6: verify that the corresponding entityId -> tppId mapping is upserted.
        verify(entityIdToTppIdMap).putAll(argThat(map ->
                map.size() == 1 && map.containsKey(tpp.getEntityId())
                        && tpp.getTppId().equals(map.get(tpp.getEntityId()))
        ));
    }

    // -------------------------------------------------------------------------
    // removeFromMap
    // -------------------------------------------------------------------------

    @Test
    void removeFromMap() {
        tppMapService.addToMap(tpp).block();

        tppMapService.removeFromMap(tpp).block();

        when(tppMap.get(tpp.getTppId())).thenReturn(Mono.empty());

        StepVerifier.create(tppMapService.getFromMap(tpp.getTppId()))
                .expectNextCount(0)
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // resetCache — lock paths
    // -------------------------------------------------------------------------

    /**
     * When the distributed lock is already held by another pod, resetCache must
     * skip the reset entirely (no MongoDB read, no Redis write).
     */
    @Test
    void resetCache_lockNotAcquired_skips() {
        when(lock.tryLock(0, -1, TimeUnit.SECONDS)).thenReturn(Mono.just(false));
        clearInvocations(tppRepository, tppMap);

        tppMapService.resetCache();

        verify(tppRepository, never()).findAll();
        verify(tppMap, never()).delete();
        verify(tppMap, never()).putAll(any());
    }

    // -------------------------------------------------------------------------
    // resetCache — buildSnapshotFromDb paths
    // -------------------------------------------------------------------------

    /**
     * When MongoDB returns no active TPPs (all filtered out), putAll must NOT be called
     * (nothing to write). The cache is not deleted — stale entries are removed individually.
     */
    @Test
    void resetCache_noActiveTpps_doesNotPutAll() {
        // Only inactive TPP in DB
        when(tppRepository.findAll()).thenReturn(Flux.just(getMockTppDisabled()));
        clearInvocations(tppMap);

        tppMapService.resetCache();

        verify(tppMap, never()).delete();
        verify(tppMap, never()).putAll(any());
    }

    /**
     * When MongoDB is completely empty, putAll must NOT be called.
     */
    @Test
    void resetCache_emptyDb_doesNotPutAll() {
        when(tppRepository.findAll()).thenReturn(Flux.empty());
        clearInvocations(tppMap);

        tppMapService.resetCache();

        verify(tppMap, never()).delete();
        verify(tppMap, never()).putAll(any());
    }

    /**
     * When decrypt fails for one TPP and succeeds for another, only the
     * successfully decrypted TPP must end up in the snapshot (putAll argument).
     */
    @Test
    void resetCache_oneDecryptFails_skipsFailedTppAndWritesOthers() {
        Tpp tppOk  = getMockTpp("tppOk",   true);
        Tpp tppErr = getMockTpp("tppErr",  true);

        when(tppRepository.findAll()).thenReturn(Flux.just(tppOk, tppErr));
        when(tokenSectionCryptService.keyDecrypt(any(), eq("tppOk")))
                .thenReturn(Mono.just(true));
        when(tokenSectionCryptService.keyDecrypt(any(), eq("tppErr")))
                .thenReturn(Mono.error(new RuntimeException("decrypt error")));
        clearInvocations(tppMap);

        tppMapService.resetCache();

        // putAll must be called with exactly 1 entry (only tppOk)
        verify(tppMap).putAll(argThat(map -> map.size() == 1 && map.containsKey("tppOk")));
    }

    // -------------------------------------------------------------------------
    // populateMap — lock paths
    // -------------------------------------------------------------------------

    /**
     * When the distributed lock is already held by another pod during startup,
     * populateMap must wait for the cache to become ready (polling isExists) without
     * reading from MongoDB or writing to the cache itself.
     */
    @Test
    void populateMap_lockNotAcquired_waitsForCacheReady() {
        // Step 1: simulate another pod holding the distributed lock.
        when(lock.tryLock(0, -1, TimeUnit.SECONDS)).thenReturn(Mono.just(false));

        // Step 2: simulate both cache structures becoming available.
        when(tppMap.isExists()).thenReturn(Mono.just(true));
        when(entityIdToTppIdMap.isExists()).thenReturn(Mono.just(true));

        clearInvocations(tppMap, entityIdToTppIdMap, tppRepository);

        // Step 3: initialize the service and wait for the cache to become ready.
        tppMapService.populateMap();

        // Step 4: this pod must not read from MongoDB or write to either cache.
        verify(tppRepository, never()).findAll();
        verify(tppMap, never()).putAll(anyMap());
        verify(entityIdToTppIdMap, never()).putAll(anyMap());
    }

    // -------------------------------------------------------------------------
    // populateMap — isExists paths
    // -------------------------------------------------------------------------

    /**
     * When the cache already exists in Redis (populated by another pod that
     * started first), populateMap must skip the DB read and putAll.
     */
    @Test
    void populateMap_cacheAlreadyExists_skips() {
        // Step 1: simulate both cache structures already populated by another pod.
        when(tppMap.isExists()).thenReturn(Mono.just(true));
        when(entityIdToTppIdMap.isExists()).thenReturn(Mono.just(true));

        clearInvocations(tppRepository, tppMap, entityIdToTppIdMap);

        // Step 2: initialization must skip the database and cache population.
        tppMapService.populateMap();

        // Step 3: verify that neither cache structure is populated again.
        verify(tppRepository, never()).findAll();
        verify(tppMap, never()).putAll(anyMap());
        verify(entityIdToTppIdMap, never()).putAll(anyMap());
    }

    /**
     * When the cache does not exist yet, populateMap must read all active TPPs
     * from MongoDB and write them atomically via putAll.
     */
    @Test
    void populateMap_cacheEmpty_populatesFromDb() {
        // isExists() → false (set in setUp), one active TPP in DB (set in setUp)
        clearInvocations(tppMap);

        tppMapService.populateMap();

        verify(tppMap).putAll(argThat(map -> map.size() == 1 && map.containsKey(tpp.getTppId())));
    }

    /**
     * When the cache does not exist but all TPPs in MongoDB are inactive,
     * populateMap must complete without calling putAll (nothing to cache).
     */
    @Test
    void populateMap_noActiveTpps_doesNotWriteToCache() {
        when(tppRepository.findAll()).thenReturn(Flux.just(getMockTppDisabled()));
        clearInvocations(tppMap);

        tppMapService.populateMap();

        verify(tppMap, never()).putAll(any());
    }

    /**
     * A TPP with {@code state == null} must be treated as inactive and excluded
     * from the cache (null-safe Boolean.TRUE.equals guard).
     */
    @Test
    void populateMap_nullStateTpp_isExcludedFromCache() {
        Tpp nullStateTpp = getMockTpp("nullStateTpp", true);
        nullStateTpp.setState(null);
        when(tppRepository.findAll()).thenReturn(Flux.just(nullStateTpp));
        clearInvocations(tppMap);

        tppMapService.populateMap();

        verify(tppMap, never()).putAll(any());
    }
}