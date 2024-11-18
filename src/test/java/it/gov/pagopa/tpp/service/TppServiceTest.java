package it.gov.pagopa.tpp.service;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.tpp.configuration.ExceptionMap;
import it.gov.pagopa.tpp.dto.mapper.TppObjectToDTOMapper;
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
import reactor.test.StepVerifier;

import static it.gov.pagopa.tpp.utils.TestUtils.*;

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

        StepVerifier.create(tppService.getEnabledList(MOCK_TPP_ID_STRING_LIST))
                .expectNextMatches(response -> response.equals(MOCK_TPP_DTO_LIST))
                .verifyComplete();
    }

    @Test
    void createTpp_AlreadyExist() {
        Mockito.when(tppRepository.findByEntityId(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        StepVerifier.create(tppService.createNewTpp(MOCK_TPP_DTO, MOCK_WRONG_ID))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_ALREADY_ONBOARDED"))
                .verify();
    }

    @Test
    void createTpp_Ok() {
        Mockito.when(tppRepository.findByEntityId(Mockito.any()))
                .thenReturn(Mono.empty());
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        StepVerifier.create(tppService.createNewTpp(MOCK_TPP_DTO, MOCK_TPP_DTO.getTppId()))
                .expectNextMatches(response -> {
                    response.setLastUpdateDate(null);
                    return response.equals(MOCK_TPP_DTO);
                })
                .verifyComplete();
    }

    @Test
    void createTpp_MissingTokenSection() {
        StepVerifier.create(tppService.createNewTpp(MOCK_TPP_DTO_NO_TOKEN_SECTION, MOCK_TPP_DTO.getTppId()))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
                .verify();
    }

    @Test
    void updateTpp_Ok() {
        Mockito.when(tppRepository.findByTppId(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        StepVerifier.create(tppService.updateExistingTpp(MOCK_TPP_DTO))
                .expectNextMatches(response -> {
                    response.setLastUpdateDate(null); // Normalize data for comparison
                    return response.equals(MOCK_TPP_DTO);
                })
                .verifyComplete();
    }

    @Test
    void updateTpp_TppNotFound() {
        Mockito.when(tppRepository.findByTppId(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(tppService.updateExistingTpp(MOCK_TPP_DTO))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
                .verify();
    }

    @Test
    void updateTpp_NoTppId() {
        StepVerifier.create(tppService.updateExistingTpp(MOCK_TPP_DTO_NO_ID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
                .verify();
    }

    @Test
    void updateState_Ok() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        StepVerifier.create(tppService.updateState(MOCK_TPP_DTO.getTppId(), MOCK_TPP_DTO.getState()))
                .expectNextMatches(result -> result.equals(MOCK_TPP_DTO))
                .verifyComplete();
    }

    @Test
    void updateState_TppNotOnboarded() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(tppService.updateState(MOCK_TPP_DTO.getTppId(), MOCK_TPP_DTO.getState()))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
                .verify();
    }

    @Test
    void get_Ok() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));

        StepVerifier.create(tppService.get(MOCK_TPP_DTO.getTppId()))
                .expectNextMatches(result -> result.equals(MOCK_TPP_DTO))
                .verifyComplete();
    }

    @Test
    void get_TppNotOnboarded() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(tppService.get(MOCK_TPP_DTO.getTppId()))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
                .verify();
    }
}


