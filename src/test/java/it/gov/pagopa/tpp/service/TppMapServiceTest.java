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
        when(lock.unlock()).thenReturn(Mono.empty());

        // Map setup
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

    @Test
    void testGetFromCache() {
        when(tppMap.get(tpp.getTppId())).thenReturn(Mono.just(tpp));

        tppMapService.addToMap(tpp).block();

        StepVerifier.create(tppMapService.getFromMap(tpp.getTppId()))
                .expectNext(tpp)
                .verifyComplete();
    }

    @Test
    void removeFromMap() {
        tppMapService.addToMap(tpp).block();

        tppMapService.removeFromMap(tpp.getTppId()).block();

        when(tppMap.get(tpp.getTppId())).thenReturn(Mono.empty());

        StepVerifier.create(tppMapService.getFromMap(tpp.getTppId()))
                .expectNextCount(0)
                .verifyComplete();
    }
}