package it.gov.pagopa.tpp.service;

import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.tpp.configuration.ExceptionMap;
import it.gov.pagopa.tpp.dto.NetworkResponseDTO;
import it.gov.pagopa.tpp.dto.mapper.TokenSectionObjectToDTOMapper;
import it.gov.pagopa.tpp.dto.mapper.TppObjectToDTOMapper;
import it.gov.pagopa.tpp.dto.mapper.TppWithoutTokenSectionObjectToDTOMapper;
import it.gov.pagopa.tpp.model.mapper.TokenSectionDTOToObjectMapper;
import it.gov.pagopa.tpp.model.mapper.TppDTOToObjectMapper;
import it.gov.pagopa.tpp.repository.TppRepository;
import it.gov.pagopa.tpp.service.keyvault.AzureKeyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.gov.pagopa.tpp.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        TppServiceImpl.class,
        TppObjectToDTOMapper.class,
        TppDTOToObjectMapper.class,
        TokenSectionObjectToDTOMapper.class,
        TokenSectionDTOToObjectMapper.class,
        TppWithoutTokenSectionObjectToDTOMapper.class,
        AzureKeyService.class,
        TppMapService.class,
        ExceptionMap.class
})
class TppServiceTest {

    @Autowired
    private TppServiceImpl tppService;

    @MockBean
    private TppRepository tppRepository;

    @MockBean
    private AzureKeyService azureKeyService;

    @MockBean
    private TokenSectionCryptService tokenSectionCryptService;

    @Autowired
    private TppDTOToObjectMapper mapperToObject;

    @Autowired
    private TokenSectionObjectToDTOMapper tokenSectionObjectToDTOMapper;

    @MockBean
    private TppMapService tppMapService;

    @Autowired
    private TokenSectionDTOToObjectMapper tokenSectionDTOToObjectMapper;

    @Mock
    private KeyVaultKey keyVault;


    @Test
    void getEnabled_Ok() {
        Mockito.when(tppRepository.findByTppIdInAndStateTrue(MOCK_TPP_ID_STRING_LIST))
                .thenReturn(Flux.fromIterable(MOCK_TPP_LIST));
        Mockito.when(tokenSectionCryptService.keyDecrypt(any(), any())).thenReturn(Mono.just(true));
        Mockito.when(tppMapService.addToMap(any())).thenReturn(Mono.just(true));

        StepVerifier.create(tppService.getEnabledList(MOCK_TPP_ID_STRING_LIST))
                .expectNextMatches(response -> response.equals(MOCK_TPP_DTO_LIST))
                .verifyComplete();
    }

    @Test
    void createTpp_AlreadyExist() {
        Mockito.when(tppRepository.findByEntityId(any()))
                .thenReturn(Mono.just(MOCK_TPP));

        StepVerifier.create(tppService.createNewTpp(MOCK_TPP_DTO, MOCK_WRONG_ID))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_ALREADY_ONBOARDED"))
                .verify();
    }

    @Test
    void createTpp_Ok() {
        Mockito.when(tppRepository.findByEntityId(any()))
                .thenReturn(Mono.empty());
        Mockito.when(tppRepository.save(any()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(azureKeyService.createRsaKey(any())).thenReturn(Mono.just(keyVault));
        Mockito.when(azureKeyService.getKey(any())).thenReturn(Mono.just(keyVault));
        Mockito.when(tokenSectionCryptService.keyEncrypt(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(tppService.createNewTpp(MOCK_TPP_DTO, MOCK_TPP_DTO.getTppId()))
                .expectNextMatches(response -> {
                    response.setLastUpdateDate(null);
                    return response.equals(MOCK_TPP_DTO);
                })
                .verifyComplete();
    }

    @Test
    void createTpp_MissingTokenSection() {
        Mockito.when(tppRepository.findByEntityId(MOCK_TPP_DTO_NO_TOKEN_SECTION.getEntityId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(tppService.createNewTpp(MOCK_TPP_DTO_NO_TOKEN_SECTION, MOCK_TPP_DTO.getTppId()))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
                .verify();
    }

    @Test
    void updateTppDetails_Ok() {
        Mockito.when(tppRepository.findByTppId(any()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        StepVerifier.create(tppService.updateTppDetails(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION))
                .expectNextMatches(response -> {
                    response.setLastUpdateDate(null);
                    return response.equals(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION);
                })
                .verifyComplete();
    }

    @Test
    void updateTppDetails_TppNotFound() {
        Mockito.when(tppRepository.findByTppId(Mockito.any()))
                .thenReturn(Mono.empty());


        StepVerifier.create(tppService.updateTppDetails(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
                .verify();
    }

    @Test
    void updateTppDetails_NoTppId() {
        StepVerifier.create(tppService.updateTppDetails(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION_NO_ID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
                .verify();
    }

    @Test
    void updateTokenSection_Ok() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(Mockito.any()))
                .thenReturn(Mono.just(MOCK_TPP));

        Mockito.when(azureKeyService.getKey(any())).thenReturn(Mono.just(keyVault));
        Mockito.when(tokenSectionCryptService.keyEncrypt(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(tppService.updateTokenSection(MOCK_TPP_DTO.getTppId(), MOCK_TOKEN_SECTION_DTO))
                .expectNextMatches(result -> result.equals(MOCK_TOKEN_SECTION_DTO))
                .verifyComplete();
      }

    @Test
    void updateTokenSection_NoTppId() {
        StepVerifier.create(tppService.updateTokenSection(null, MOCK_TOKEN_SECTION_DTO))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
                .verify();
    }

    @Test
    void updateTokenSection_TppNotFound() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(tppService.updateTokenSection(MOCK_TPP_DTO.getTppId(), MOCK_TOKEN_SECTION_DTO))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
                .verify();
    }

    @Test
    void updateState_Ok() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(any()))
                .thenReturn(Mono.just(MOCK_TPP));
        StepVerifier.create(tppService.updateState(MOCK_TPP_DTO.getTppId(), MOCK_TPP_DTO.getState()))
                .expectNextMatches(result -> result.getTppId().equals(MOCK_TPP_DTO.getTppId()))
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
    void updateIsPaymentEnabled_Ok() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.save(any()))
                .thenReturn(Mono.just(MOCK_TPP));
        StepVerifier.create(tppService.updateIsPaymentEnabled(MOCK_TPP_DTO.getTppId(), MOCK_IS_PAYMENT_ENABLED.getIsPaymentEnabled()))
                .expectNextMatches(result -> {
                                return result.getTppId().equals(MOCK_TPP_DTO.getTppId()) &&
                                        result.getIsPaymentEnabled().equals(MOCK_IS_PAYMENT_ENABLED.getIsPaymentEnabled());
                        })
                .verifyComplete();
    }

    @Test
    void updateIsPaymentEnabled_Ok_TppNotOnboarded() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(tppService.updateIsPaymentEnabled(MOCK_TPP_DTO.getTppId(), MOCK_IS_PAYMENT_ENABLED.getIsPaymentEnabled()))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
                .verify();
    }

    @Test
    void getTppDetails_Ok() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));

        StepVerifier.create(tppService.getTppDetails(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getTppId()))
                .expectNextMatches(result -> result.getTppId().equals(MOCK_TPP_DTO.getTppId()))
                .verifyComplete();
    }

    @Test
    void getTppDetails_TppNotOnboarded() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getTppId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(tppService.getTppDetails(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getTppId()))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
                .verify();
    }

    @Test
    void getTppByEntityId_Ok() {
        Mockito.when(tppRepository.findByEntityId(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getEntityId()))
                .thenReturn(Mono.just(MOCK_TPP));

        StepVerifier.create(tppService.getTppByEntityId(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getEntityId()))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getTppByEntityId_TppNotOnboarded() {
        Mockito.when(tppRepository.findByEntityId(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getEntityId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(tppService.getTppByEntityId(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getEntityId()))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientExceptionWithBody &&
                                ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
                .verify();
    }

    @Test
    void getTokenSection_Ok() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tokenSectionCryptService.keyDecrypt(any(), any())).thenReturn(Mono.just(true));
        StepVerifier.create(tppService.getTokenSection(MOCK_TPP_DTO.getTppId()))
                .expectNextMatches(result -> result.equals(MOCK_TOKEN_SECTION_DTO))
                .verifyComplete();
    }

    @Test
    void getTokenSection_TppNotFound() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(tppService.getTokenSection(MOCK_TPP_DTO.getTppId()))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Tpp not found during get process"))
                .verify();
    }


    @Test
    void testConnection(){
        NetworkResponseDTO networkResponseDTO = new NetworkResponseDTO();
        networkResponseDTO.setMessage("tppName ha raggiunto i nostri sistemi");
        networkResponseDTO.setCode("PAGOPA_NETWORK_TEST");
        StepVerifier.create(tppService.testConnection("tppName"))
                .expectNext(networkResponseDTO)
                .verifyComplete();
    }

    @Test
    void deleteTpp_OK() {
        Mockito.when(tppRepository.findByTppId(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP));
        Mockito.when(tppRepository.delete(MOCK_TPP)).thenReturn(Mono.empty());

        StepVerifier.create(tppService.deleteTpp(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getTppId()))
                .expectNextCount(1)
                .verifyComplete();
    }

}


