package it.gov.pagopa.tpp.service;

import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.tpp.configuration.ExceptionMap;
import it.gov.pagopa.tpp.dto.NetworkResponseDTO;
import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.dto.mapper.TokenSectionObjectToDTOMapper;
import it.gov.pagopa.tpp.dto.mapper.TppObjectToDTOMapper;
import it.gov.pagopa.tpp.dto.mapper.TppWithoutTokenSectionObjectToDTOMapper;
import it.gov.pagopa.tpp.model.Tpp;
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
        Mockito.when(tppRepository.findByTppIdInAndStateTrue(getMockTppIdStringList()))
            .thenReturn(Flux.fromIterable(getMockTppList()));
        Mockito.when(tokenSectionCryptService.keyDecrypt(any(), any())).thenReturn(Mono.just(true));
        Mockito.when(tppMapService.addToMap(any())).thenReturn(Mono.just(true));

        StepVerifier.create(tppService.getEnabledList(getMockTppIdStringList()))
            .expectNextMatches(response -> response.equals(getMockTppDtoList()))
            .verifyComplete();
    }

    @Test
    void createTpp_AlreadyExist() {
        Mockito.when(tppRepository.findByEntityId(any()))
            .thenReturn(Mono.just(getMockTpp()));

        StepVerifier.create(tppService.createNewTpp(getMockTppDto(), MOCK_WRONG_ID))
            .expectErrorMatches(throwable ->
                throwable instanceof ClientExceptionWithBody &&
                    ((ClientExceptionWithBody) throwable).getCode().equals("TPP_ALREADY_ONBOARDED"))
            .verify();
    }

    @Test
    void createTpp_Ok() {
        TppDTO inputDto = getMockTppDto();
        Tpp mockTppEntity = getMockTpp();

        Mockito.when(tppRepository.findByEntityId(any()))
            .thenReturn(Mono.empty());
        Mockito.when(tppRepository.save(any()))
            .thenReturn(Mono.just(mockTppEntity));
        Mockito.when(azureKeyService.createRsaKey(any())).thenReturn(Mono.just(keyVault));
        Mockito.when(azureKeyService.getKey(any())).thenReturn(Mono.just(keyVault));
        Mockito.when(tokenSectionCryptService.keyEncrypt(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(tppService.createNewTpp(inputDto, inputDto.getTppId()))
            .expectNextMatches(response -> {
                response.setLastUpdateDate(null);
                return response.equals(getMockTppDto());
            })
            .verifyComplete();
    }

    @Test
    void createTpp_MissingTokenSection() {
        TppDTO tppNoToken = getMockTppDtoNoTokenSection();
        TppDTO tppDto = getMockTppDto();

        Mockito.when(tppRepository.findByEntityId(tppNoToken.getEntityId()))
            .thenReturn(Mono.empty());

        StepVerifier.create(tppService.createNewTpp(tppNoToken, tppDto.getTppId()))
            .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
            .verify();
    }

    @Test
    void updateTppDetails_Ok() {
        TppDTOWithoutTokenSection inputDto = getMockTppDtoWithoutTokenSection();
        Tpp mockTpp = getMockTpp();

        Mockito.when(tppRepository.findByTppId(any()))
            .thenReturn(Mono.just(mockTpp));
        Mockito.when(tppRepository.save(Mockito.any()))
            .thenReturn(Mono.just(mockTpp));

        StepVerifier.create(tppService.updateTppDetails(inputDto))
            .expectNextMatches(response -> {
                response.setLastUpdateDate(null);
                return response.equals(getMockTppDtoWithoutTokenSection());
            })
            .verifyComplete();
    }

    @Test
    void updateTppDetails_TppNotFound() {
        Mockito.when(tppRepository.findByTppId(Mockito.any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(tppService.updateTppDetails(getMockTppDtoWithoutTokenSection()))
            .expectErrorMatches(throwable ->
                throwable instanceof ClientExceptionWithBody &&
                    ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
            .verify();
    }

    @Test
    void updateTppDetails_NoTppId() {
        StepVerifier.create(tppService.updateTppDetails(getMockTppDtoWithoutTokenSectionNoId()))
            .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
            .verify();
    }

    @Test
    void updateTokenSection_Ok() {
        TppDTO tppDto = getMockTppDto();
        Tpp mockTpp = getMockTpp();
        TokenSectionDTO tokenSectionDTO = getMockTokenSectionDto();

        Mockito.when(tppRepository.findByTppId(tppDto.getTppId()))
            .thenReturn(Mono.just(mockTpp));
        Mockito.when(tppRepository.save(Mockito.any()))
            .thenReturn(Mono.just(mockTpp));

        Mockito.when(azureKeyService.getKey(any())).thenReturn(Mono.just(keyVault));
        Mockito.when(tokenSectionCryptService.keyEncrypt(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(tppService.updateTokenSection(tppDto.getTppId(), tokenSectionDTO))
            .expectNextMatches(result -> result.equals(getMockTokenSectionDto()))
            .verifyComplete();
    }

    @Test
    void updateTokenSection_NoTppId() {
        StepVerifier.create(tppService.updateTokenSection(null, getMockTokenSectionDto()))
            .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
            .verify();
    }

    @Test
    void updateTokenSection_TppNotFound() {
        TppDTO tppDto = getMockTppDto();
        TokenSectionDTO tokenSectionDTO = getMockTokenSectionDto();

        Mockito.when(tppRepository.findByTppId(tppDto.getTppId()))
            .thenReturn(Mono.empty());

        StepVerifier.create(tppService.updateTokenSection(tppDto.getTppId(), tokenSectionDTO))
            .expectErrorMatches(throwable ->
                throwable instanceof ClientExceptionWithBody &&
                    ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
            .verify();
    }

    @Test
    void updateState_Ok() {
        TppDTO tppDto = getMockTppDto();
        Tpp mockTpp = getMockTpp();

        Mockito.when(tppRepository.findByTppId(tppDto.getTppId()))
            .thenReturn(Mono.just(mockTpp));
        Mockito.when(tppRepository.save(any()))
            .thenReturn(Mono.just(mockTpp));

        StepVerifier.create(tppService.updateState(tppDto.getTppId(), tppDto.getState()))
            .expectNextMatches(result -> result.getTppId().equals(tppDto.getTppId()))
            .verifyComplete();
    }

    @Test
    void updateState_TppNotOnboarded() {
        TppDTO tppDto = getMockTppDto();

        Mockito.when(tppRepository.findByTppId(tppDto.getTppId()))
            .thenReturn(Mono.empty());

        StepVerifier.create(tppService.updateState(tppDto.getTppId(), tppDto.getState()))
            .expectErrorMatches(throwable ->
                throwable instanceof ClientExceptionWithBody &&
                    ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
            .verify();
    }

    @Test
    void updateIsPaymentEnabled_Ok() {
        TppDTO tppDto = getMockTppDto();
        Tpp mockTpp = getMockTpp();
        var isPaymentEnabled = getMockIsPaymentEnabled();

        Mockito.when(tppRepository.findByTppId(tppDto.getTppId()))
            .thenReturn(Mono.just(mockTpp));
        Mockito.when(tppRepository.save(any()))
            .thenReturn(Mono.just(mockTpp));

        StepVerifier.create(tppService.updateIsPaymentEnabled(tppDto.getTppId(), isPaymentEnabled.getIsPaymentEnabled()))
            .expectNextMatches(result -> {
                return result.getTppId().equals(tppDto.getTppId()) &&
                    result.getIsPaymentEnabled().equals(isPaymentEnabled.getIsPaymentEnabled());
            })
            .verifyComplete();
    }

    @Test
    void updateIsPaymentEnabled_Ok_TppNotOnboarded() {
        TppDTO tppDto = getMockTppDto();
        var isPaymentEnabled = getMockIsPaymentEnabled();

        Mockito.when(tppRepository.findByTppId(tppDto.getTppId()))
            .thenReturn(Mono.empty());

        StepVerifier.create(tppService.updateIsPaymentEnabled(tppDto.getTppId(), isPaymentEnabled.getIsPaymentEnabled()))
            .expectErrorMatches(throwable ->
                throwable instanceof ClientExceptionWithBody &&
                    ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
            .verify();
    }

    @Test
    void getTppDetails_Ok() {
        TppDTOWithoutTokenSection tppDtoNoToken = getMockTppDtoWithoutTokenSection();
        TppDTO tppDto = getMockTppDto();

        Mockito.when(tppRepository.findByTppId(tppDtoNoToken.getTppId()))
            .thenReturn(Mono.just(getMockTpp()));

        StepVerifier.create(tppService.getTppDetails(tppDtoNoToken.getTppId()))
            .expectNextMatches(result -> result.getTppId().equals(tppDto.getTppId()))
            .verifyComplete();
    }

    @Test
    void getTppDetails_TppNotOnboarded() {
        TppDTOWithoutTokenSection tppDtoNoToken = getMockTppDtoWithoutTokenSection();

        Mockito.when(tppRepository.findByTppId(tppDtoNoToken.getTppId()))
            .thenReturn(Mono.empty());

        StepVerifier.create(tppService.getTppDetails(tppDtoNoToken.getTppId()))
            .expectErrorMatches(throwable ->
                throwable instanceof ClientExceptionWithBody &&
                    ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
            .verify();
    }

    @Test
    void getTppByEntityId_Ok() {
        TppDTOWithoutTokenSection tppDtoNoToken = getMockTppDtoWithoutTokenSection();

        Mockito.when(tppRepository.findByEntityId(tppDtoNoToken.getEntityId()))
            .thenReturn(Mono.just(getMockTpp()));

        StepVerifier.create(tppService.getTppByEntityId(tppDtoNoToken.getEntityId()))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void getTppByEntityId_TppNotOnboarded() {
        TppDTOWithoutTokenSection tppDtoNoToken = getMockTppDtoWithoutTokenSection();

        Mockito.when(tppRepository.findByEntityId(tppDtoNoToken.getEntityId()))
            .thenReturn(Mono.empty());

        StepVerifier.create(tppService.getTppByEntityId(tppDtoNoToken.getEntityId()))
            .expectErrorMatches(throwable ->
                throwable instanceof ClientExceptionWithBody &&
                    ((ClientExceptionWithBody) throwable).getCode().equals("TPP_NOT_ONBOARDED"))
            .verify();
    }

    @Test
    void getTokenSection_Ok() {
        TppDTO tppDto = getMockTppDto();

        Mockito.when(tppRepository.findByTppId(tppDto.getTppId()))
            .thenReturn(Mono.just(getMockTpp()));
        Mockito.when(tokenSectionCryptService.keyDecrypt(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(tppService.getTokenSection(tppDto.getTppId()))
            .expectNextMatches(result -> result.equals(getMockTokenSectionDto()))
            .verifyComplete();
    }

    @Test
    void getTokenSection_TppNotFound() {
        TppDTO tppDto = getMockTppDto();

        Mockito.when(tppRepository.findByTppId(tppDto.getTppId()))
            .thenReturn(Mono.empty());

        StepVerifier.create(tppService.getTokenSection(tppDto.getTppId()))
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
        TppDTOWithoutTokenSection tppDto = getMockTppDtoWithoutTokenSection();
        Tpp mockTpp = getMockTpp();

        Mockito.when(tppRepository.findByTppId(tppDto.getTppId()))
            .thenReturn(Mono.just(mockTpp));

        Mockito.when(tppRepository.delete(mockTpp)).thenReturn(Mono.empty());

        StepVerifier.create(tppService.deleteTpp(tppDto.getTppId()))
            .expectNextCount(1)
            .verifyComplete();
    }
}