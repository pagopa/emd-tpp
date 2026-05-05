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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

    @Mock
    private RLockReactive lock;

    @MockBean
    private RedissonReactiveClient redissonClient;

    @MockBean
    private TppRepository tppRepository;

    @MockBean
    private TokenSectionCryptService tokenSectionCryptService;

    private TppMapService tppMapService;

    private Tpp tpp;

    @BeforeEach
    void setUp() {
        tpp = getMockTpp();
        tppMap = mock(RMapReactive.class);
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
        when(tppMap.putAll(any())).thenReturn(Mono.empty());

        // Repository and crypto
        when(tppRepository.findAll()).thenReturn(Flux.just(tpp));
        when(tokenSectionCryptService.keyDecrypt(any(TokenSection.class), anyString()))
                .thenReturn(Mono.just(true));

        tppMapService = new TppMapService(tppRepository, tokenSectionCryptService, redissonClient, tppMap);
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
    // removeFromMap
    // -------------------------------------------------------------------------

    @Test
    void removeFromMap() {
        tppMapService.addToMap(tpp).block();

        tppMapService.removeFromMap(tpp.getTppId()).block();

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
     * When MongoDB returns no active TPPs (all filtered out), the cache must be
     * deleted but putAll must NOT be called (nothing to write).
     */
    @Test
    void resetCache_noActiveTpps_deletesMapWithoutPutAll() {
        // Only inactive TPP in DB
        when(tppRepository.findAll()).thenReturn(Flux.just(getMockTppDisabled()));
        clearInvocations(tppMap);

        tppMapService.resetCache();

        verify(tppMap).delete();
        verify(tppMap, never()).putAll(any());
    }

    /**
     * When MongoDB is completely empty, the cache must be deleted but putAll
     * must NOT be called.
     */
    @Test
    void resetCache_emptyDb_deletesMapWithoutPutAll() {
        when(tppRepository.findAll()).thenReturn(Flux.empty());
        clearInvocations(tppMap);

        tppMapService.resetCache();

        verify(tppMap).delete();
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
     * populateMap must skip entirely (no isExists check, no DB read).
     */
    @Test
    void populateMap_lockNotAcquired_skips() {
        when(lock.tryLock(0, -1, TimeUnit.SECONDS)).thenReturn(Mono.just(false));
        clearInvocations(tppMap, tppRepository);

        tppMapService.populateMap();

        verify(tppMap, never()).isExists();
        verify(tppRepository, never()).findAll();
        verify(tppMap, never()).putAll(any());
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
        when(tppMap.isExists()).thenReturn(Mono.just(true));
        clearInvocations(tppRepository, tppMap);

        tppMapService.populateMap();

        verify(tppRepository, never()).findAll();
        verify(tppMap, never()).putAll(any());
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