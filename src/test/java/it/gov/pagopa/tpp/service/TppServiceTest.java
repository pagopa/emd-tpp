package it.gov.pagopa.tpp.service;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.mapper.TppObjectToDTOMapper;
import it.gov.pagopa.tpp.configuration.ExceptionMap;
import it.gov.pagopa.tpp.faker.TppDTOFaker;
import org.junit.jupiter.api.function.Executable;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.model.mapper.TppDTOToObjectMapper;
import it.gov.pagopa.tpp.repository.TppRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {
        TppServiceImpl.class,
        TppObjectToDTOMapper.class,
        TppDTOToObjectMapper.class,
        ExceptionMap.class
})
class TppServiceTest {
    @Autowired
    TppServiceImpl tppService;
    @MockBean
    TppRepository tppRepository;
    @Autowired
    TppDTOToObjectMapper mapperToObject;

    @Test
    void getEnabled_Ok() {

        TppDTO tppDTO = TppDTOFaker.mockInstance(true);
        List<TppDTO> tppDTOs =List.of(tppDTO);
        List<Tpp> tpps =List.of(mapperToObject.map(tppDTO));
        List<String> arrayList = List.of("1");

        Mockito.when(tppRepository.findByTppIdInAndStateTrue(arrayList))
                .thenReturn(Flux.fromIterable(tpps));

        List<TppDTO> response = tppService.getEnabledList(arrayList).block();

        assertEquals(tppDTOs, response);

    }

    @Test
    void createTpp_Ok() {

        TppDTO tppDTO = TppDTOFaker.mockInstance(true);
        Tpp tpp = mapperToObject.map(tppDTO);

        Mockito.when(tppRepository.findByTppId(Mockito.any()))
                .thenReturn(Mono.empty());
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(tpp));

        TppDTO response = tppService.upsert(tppDTO).block();

        assertEquals(tppDTO, response);
    }

    @Test
    void updateTpp_Ok() {

        TppDTO tppDTO = TppDTOFaker.mockInstance(true);
        Tpp tpp = mapperToObject.map(tppDTO);

        Mockito.when(tppRepository.findByTppId(Mockito.any()))
                .thenReturn(Mono.just(tpp));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(tpp));

        TppDTO response = tppService.upsert(tppDTO).block();

        assertEquals(tppDTO, response);
    }



    @Test
    void updateState_Ok() {

        TppDTO tppDTO = TppDTOFaker.mockInstance(true);
        Tpp tpp = mapperToObject.map(tppDTO);

        Mockito.when(tppRepository.findByTppId(tppDTO.getTppId()))
                .thenReturn(Mono.just(tpp));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(tpp));

        TppDTO result = tppService.updateState(tppDTO.getTppId(),tppDTO.getState()).block();

        assertEquals(result, tppDTO);
    }

    @Test
    void updateState_Ko_TppNotOnboarded() {

        TppDTO tppDTO = TppDTOFaker.mockInstance(true);

        Mockito.when(tppRepository.findByTppId(tppDTO.getTppId()))
                .thenReturn(Mono.empty());

        Executable executable = () -> tppService.updateState(tppDTO.getTppId(),tppDTO.getState()).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertEquals("TPP_NOT_ONBOARDED", exception.getCode());
    }

    @Test
    void get_Ok() {

        TppDTO tppDTO = TppDTOFaker.mockInstance(true);
        Tpp tpp = mapperToObject.map(tppDTO);

        Mockito.when(tppRepository.findByTppId(tppDTO.getEntityId()))
                .thenReturn(Mono.just(tpp));

        TppDTO result = tppService.get(tppDTO.getEntityId()).block();

        assertEquals(result, tppDTO);
    }

    @Test
    void get_Ko_TppNotOnboarded() {

        TppDTO tppDTO = TppDTOFaker.mockInstance(true);

        Mockito.when(tppRepository.findByTppId(tppDTO.getEntityId()))
                .thenReturn(Mono.empty());

        Executable executable = () -> tppService.get(tppDTO.getEntityId()).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertEquals("TPP_NOT_ONBOARDED", exception.getCode());
    }
}
