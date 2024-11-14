package it.gov.pagopa.tpp.service;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.tpp.configuration.ExceptionMap;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.mapper.TppObjectToDTOMapper;
import it.gov.pagopa.tpp.model.mapper.TppDTOToObjectMapper;
import it.gov.pagopa.tpp.repository.TppRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.gov.pagopa.tpp.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        TppServiceImpl.class,
        TppObjectToDTOMapper.class,
        TppDTOToObjectMapper.class,
        ExceptionMap.class
})
class TppServiceTest {

    @Autowired
    private TppServiceImpl tppService;

    @MockBean
    private TppRepository tppRepository;


    @Autowired
    private TppDTOToObjectMapper mapperToObject;



    @Test
    void getEnabled_Ok() {
        Mockito.when(tppRepository.findByTppIdInAndStateTrue(MOCK_TPP_ID_STRING_LIST))
                .thenReturn(Flux.fromIterable(MOCK_TPP_LIST));

        List<TppDTO> response = tppService.getEnabledList(MOCK_TPP_ID_STRING_LIST).block();

        assertNotNull(response);
        assertEquals(MOCK_TPP_DTO_LIST, response);
    }

    @Test
    void createTpp_AlreadyExist(){
        Mockito.when(tppRepository.findByEntityId(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        Executable executable = () -> tppService.createNewTpp(MOCK_TPP_DTO,MOCK_WRONG_ID).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertNotNull(exception);
        assertEquals("TPP_ALREADY_ONBOARDED", exception.getCode());
    }

    @Test
    void createTpp_Ok() {

        Mockito.when(tppRepository.findByEntityId(Mockito.any()))
                .thenReturn(Mono.empty());
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        TppDTO response = tppService.createNewTpp(MOCK_TPP_DTO, MOCK_TPP_DTO.getTppId()).block();

        assertNotNull(response);
        response.setLastUpdateDate(null);
        assertEquals(MOCK_TPP_DTO, response);
    }

    @Test
    void updateTpp_Ok() {
        Mockito.when(tppRepository.findByTppId(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        TppDTO response = tppService.updateExistingTpp(MOCK_TPP_DTO).block();

        assertNotNull(response);
        response.setLastUpdateDate(null);
        assertEquals(MOCK_TPP_DTO, response);
    }

    @Test
    void updateTpp_TppNotFound() {
        Mockito.when(tppRepository.findByTppId(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.empty());

        Executable executable = () -> tppService.updateExistingTpp(MOCK_TPP_DTO).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertNotNull(exception);
        assertEquals("TPP_NOT_ONBOARDED", exception.getCode());
    }

    @Test
    void updateTpp_NoTppId(){
        Executable executable = () -> tppService.updateExistingTpp(MOCK_TPP_DTO_NO_ID).block();
        RuntimeException exception = assertThrows(RuntimeException.class, executable);

        assertNotNull(exception);
    }

    @Test
    void updateState_Ok() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        TppDTO result = tppService.updateState(MOCK_TPP_DTO.getTppId(), MOCK_TPP_DTO.getState()).block();

        assertNotNull(result);
        assertEquals(MOCK_TPP_DTO, result);
    }

    @Test
    void updateState_Ko_TppNotOnboarded() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.empty());

        Executable executable = () -> tppService.updateState(MOCK_TPP_DTO.getTppId(), MOCK_TPP_DTO.getState()).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertNotNull(exception);
        assertEquals("TPP_NOT_ONBOARDED", exception.getCode());
    }

    @Test
    void get_Ok() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));

        TppDTO result = tppService.get(MOCK_TPP_DTO.getTppId()).block();

        assertNotNull(result);
        assertEquals(MOCK_TPP_DTO, result);
    }

    @Test
    void get_Ko_TppNotOnboarded() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.empty());

        Executable executable = () -> tppService.get(MOCK_TPP_DTO.getTppId()).block();
        ClientExceptionWithBody exception = assertThrows(ClientExceptionWithBody.class, executable);

        assertNotNull(exception);
        assertEquals("TPP_NOT_ONBOARDED", exception.getCode());
    }
}
