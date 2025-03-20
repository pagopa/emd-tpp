package it.gov.pagopa.tpp.service;


import com.github.benmanes.caffeine.cache.Caffeine;
import it.gov.pagopa.tpp.model.TokenSection;
import it.gov.pagopa.tpp.repository.TppRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    @BeforeEach
    void setUp(){
        when(tppRepository.findAll()).thenReturn(Flux.just(MOCK_TPP,MOCK_TPP));
        when(tokenSectionCryptService.keyDecrypt(any(TokenSection.class),anyString())).thenReturn(Mono.just(true));
        tppMapService = new TppMapService(tppRepository, tokenSectionCryptService);
        tppMapService.resetCache();
    }


    @Test
    void testGetFromCache(){
        assertEquals(MOCK_TPP, tppMapService.getFromMap("tppId"));
    }

    @Test
    void removeFromMap(){
        tppMapService.removeFromMap("tppId");
        assertNull(tppMapService.getFromMap("tppId"));
    }

}
