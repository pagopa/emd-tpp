package it.gov.pagopa.tpp.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import it.gov.pagopa.tpp.model.TokenSection;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.gov.pagopa.tpp.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    TokenSectionCryptService.class,
    TppRepository.class,
    Caffeine.class
})
class TppMapServiceTest {

    @MockBean
    private TppRepository tppRepository;

    @MockBean
    private TokenSectionCryptService tokenSectionCryptService;

    private TppMapService tppMapService;

    private Tpp tpp;

    @BeforeEach
    void setUp(){
        tpp = getMockTpp();

        when(tppRepository.findAll()).thenReturn(Flux.just(tpp, tpp));

        when(tokenSectionCryptService.keyDecrypt(any(TokenSection.class), anyString()))
            .thenReturn(Mono.just(true));

        tppMapService = new TppMapService(tppRepository, tokenSectionCryptService);
        tppMapService.resetCache();
    }

    @Test
    void testGetFromCache() {
        tppMapService.addToMap(tpp).block();

        StepVerifier.create(tppMapService.getFromMap(tpp.getTppId()))
            .expectNext(tpp)
            .verifyComplete();
    }

    @Test
    void removeFromMap() {
        tppMapService.addToMap(tpp).block();

        tppMapService.removeFromMap(tpp.getTppId());

        StepVerifier.create(tppMapService.getFromMap(tpp.getTppId()))
            .expectNextCount(0)
            .verifyComplete();
    }
}