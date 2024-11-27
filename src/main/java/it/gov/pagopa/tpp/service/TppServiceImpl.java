package it.gov.pagopa.tpp.service;

import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.configuration.ExceptionMap;
import it.gov.pagopa.tpp.constants.TppConstants.ExceptionMessage;
import it.gov.pagopa.tpp.constants.TppConstants.ExceptionName;
import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.dto.mapper.TokenSectionObjectToDTOMapper;
import it.gov.pagopa.tpp.dto.mapper.TppObjectToDTOMapper;
import it.gov.pagopa.tpp.dto.mapper.TppWithoutTokenSectionObjectToDTOMapper;
import it.gov.pagopa.tpp.model.TokenSection;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.model.mapper.TokenSectionDTOToObjectMapper;
import it.gov.pagopa.tpp.model.mapper.TppDTOToObjectMapper;
import it.gov.pagopa.tpp.repository.TppRepository;
import it.gov.pagopa.tpp.service.keyvault.AzureEncryptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static it.gov.pagopa.common.utils.Utils.inputSanify;


@Service
@Slf4j
public class TppServiceImpl implements TppService {

    private final TppRepository tppRepository;
    private final TppObjectToDTOMapper mapperToDTO;
    private final TppWithoutTokenSectionObjectToDTOMapper tppWithoutTokenSectionMapperToDTO;
    private final TokenSectionObjectToDTOMapper tokenSectionMapperToDTO;
    private final TppDTOToObjectMapper mapperToObject;
    private final TokenSectionDTOToObjectMapper tokenSectionMapperToObject;
    private final ExceptionMap exceptionMap;
    private final AzureEncryptService azureEncryptService;

    public TppServiceImpl(TppRepository tppRepository, TppObjectToDTOMapper mapperToDTO, TppWithoutTokenSectionObjectToDTOMapper tppWithoutTokenSectionMapperToDTO, TokenSectionObjectToDTOMapper tokenSectionMapperToDTO,
                          TppDTOToObjectMapper mapperToObject, TokenSectionDTOToObjectMapper tokenSectionMapperToObject, ExceptionMap exceptionMap, AzureEncryptService azureEncryptService) {
        this.tppRepository = tppRepository;
        this.mapperToDTO = mapperToDTO;
        this.tppWithoutTokenSectionMapperToDTO = tppWithoutTokenSectionMapperToDTO;
        this.tokenSectionMapperToDTO = tokenSectionMapperToDTO;
        this.mapperToObject = mapperToObject;
        this.tokenSectionMapperToObject = tokenSectionMapperToObject;
        this.exceptionMap = exceptionMap;
        this.azureEncryptService = azureEncryptService;
    }

    @Override
    public Mono<List<TppDTO>> getEnabledList(List<String> tppIdList) {
        log.info("[TPP-SERVICE][GET-ENABLED] Received tppIdList: {}", tppIdList);

        return tppRepository.findByTppIdInAndStateTrue(tppIdList)
                 .map(tpp -> { keyDecrypt(tpp.getTokenSection(),tpp.getTppId());
                                      return mapperToDTO.map(tpp);})
                .collectList()
                .doOnSuccess(tppDTOList -> log.info("[TPP-SERVICE][GET-ENABLED] Found TPPs: {}", tppDTOList))
                .doOnError(error -> log.error("[TPP-SERVICE][GET-ENABLED] Error retrieving enabled TPPs: {}", error.getMessage(), error));
    }

    @Override
    public Mono<TppDTOWithoutTokenSection> updateTppDetails(TppDTOWithoutTokenSection tppDTOWithoutTokenSection) {
        if (tppDTOWithoutTokenSection.getTppId() == null)
            return Mono.error(exceptionMap.throwException(ExceptionName.GENERIC_ERROR,
                    ExceptionMessage.GENERIC_ERROR));

        return tppRepository.findByTppId(tppDTOWithoutTokenSection.getTppId())
                .flatMap(existingTpp -> {
                    log.info("[TPP-SERVICE][UPSERT] TPP with tppId [{}] already exists. Updating...", tppDTOWithoutTokenSection.getTppId());
                    existingTpp.setLastUpdateDate(LocalDateTime.now());
                    existingTpp.setMessageUrl(tppDTOWithoutTokenSection.getMessageUrl());
                    existingTpp.setContact(tppDTOWithoutTokenSection.getContact());
                    existingTpp.setBusinessName(tppDTOWithoutTokenSection.getBusinessName());
                    existingTpp.setLegalAddress(tppDTOWithoutTokenSection.getLegalAddress());
                    return tppRepository.save(existingTpp)
                            .map(tppWithoutTokenSectionMapperToDTO::map)
                            .doOnSuccess(savedTpp -> log.info("[TPP-SERVICE][UPSERT] Updated existing TPP with tppId: {}", existingTpp.getTppId()))
                            .doOnError(error -> log.error("[TPP-SERVICE][SAVE] Error saving TPP with tppId {}: {}", existingTpp.getTppId(), error.getMessage()));
                })
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        ExceptionMessage.TPP_NOT_ONBOARDED)));
    }

    @Override
    public Mono<TokenSectionDTO> updateTokenSection(String tppId, TokenSectionDTO tokenSectionDTO) {
        if (tppId == null)
            return Mono.error(exceptionMap.throwException(ExceptionName.GENERIC_ERROR,
                    ExceptionMessage.GENERIC_ERROR));

        return tppRepository.findByTppId(tppId)
                .flatMap(existingTpp -> {
                    log.info("[TPP-SERVICE][UPDATE] Updating TokenSection for TPP with tppId: {}", tppId);
                    existingTpp.setLastUpdateDate(LocalDateTime.now());
                    existingTpp.setTokenSection(tokenSectionMapperToObject.map(tokenSectionDTO));
                    return tppRepository.save(existingTpp)
                            .map(tpp -> tokenSectionMapperToDTO.map(tpp.getTokenSection()))
                            .doOnSuccess(updatedTokenSection -> log.info("[TPP-SERVICE][UPDATE] Updated TokenSection for tppId: {}", tppId))
                            .doOnError(error -> log.error("[TPP-SERVICE][UPDATE] Error updating TokenSection for tppId {}: {}", tppId, error.getMessage()));
                })
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        ExceptionMessage.TPP_NOT_ONBOARDED)));
    }

    @Override
    public Mono<TppDTO> createNewTpp(TppDTO tppDTO, String tppId) {

         return tppRepository.findByEntityId(tppDTO.getEntityId())
                .switchIfEmpty(Mono.defer(() -> createAndSaveNewTpp(tppDTO, tppId)))
                .flatMap(result -> {
                    if (!result.getTppId().equals(tppId)) {
                        return Mono.error(exceptionMap.throwException(ExceptionName.TPP_ALREADY_ONBOARDED,
                                ExceptionMessage.TPP_ALREADY_ONBOARDED));
                    }
                    return Mono.just(mapperToDTO.map(result));
                });
    }

    private Mono<Tpp> createAndSaveNewTpp(TppDTO tppDTO, String tppId) {
        log.info("[TPP-SERVICE][UPSERT] Creating new entry with generated tppId: {}", tppId);
        KeyVaultKey keyVaultKey = azureEncryptService.createRsaKey(tppId);
        keyEncrypt(tppDTO.getTokenSection(),keyVaultKey);
        Tpp tppToSave = mapperToObject.map(tppDTO);
        tppToSave.setTppId(tppId);
        tppToSave.setLastUpdateDate(LocalDateTime.now());
        tppToSave.setCreationDate(LocalDateTime.now());
        return tppRepository.save(tppToSave)
                .doOnSuccess(savedTpp -> log.info("[TPP-SERVICE][UPSERT] Created new TPP with tppId: {}", tppToSave.getTppId()))
                .doOnError(error -> log.error("[TPP-SERVICE][SAVE] Error saving TPP with tppId {}: {}", tppToSave.getTppId(), error.getMessage()));
    }

    private void keyEncrypt(TokenSection tokenSection,KeyVaultKey keyVaultKey) {
        CryptographyClient cryptographyClient = azureEncryptService.buildCryptographyClient(keyVaultKey);
        if(tokenSection.getPathAdditionalProperties() != null && !tokenSection.getBodyAdditionalProperties().isEmpty()){
            tokenSection.getPathAdditionalProperties().replaceAll((key, value) -> azureEncryptService.encrypt(value.getBytes(), EncryptionAlgorithm.RSA_OAEP_256,cryptographyClient));
        }
        if(tokenSection.getBodyAdditionalProperties() != null && !tokenSection.getBodyAdditionalProperties().isEmpty()){
            tokenSection.getBodyAdditionalProperties().replaceAll((key, value) -> azureEncryptService.encrypt(value.getBytes(), EncryptionAlgorithm.RSA_OAEP_256,cryptographyClient));
        }
    }

    private void keyDecrypt(TokenSection tokenSection,String tppId) {
        KeyVaultKey keyVaultKey = azureEncryptService.getKey(tppId);
        CryptographyClient cryptographyClient = azureEncryptService.buildCryptographyClient(keyVaultKey);
        if(tokenSection.getPathAdditionalProperties() != null && !tokenSection.getBodyAdditionalProperties().isEmpty()){
            tokenSection.getPathAdditionalProperties().replaceAll((key, value) -> azureEncryptService.decrypt(value, EncryptionAlgorithm.RSA_OAEP_256,cryptographyClient));
        }
        if(tokenSection.getBodyAdditionalProperties() != null && !tokenSection.getBodyAdditionalProperties().isEmpty()){
            tokenSection.getBodyAdditionalProperties().replaceAll((key, value) -> azureEncryptService.decrypt(value, EncryptionAlgorithm.RSA_OAEP_256,cryptographyClient));
        }
    }

    @Override
    public Mono<TppDTO> updateState(String tppId, Boolean state) {
        log.info("[TPP-SERVICE][UPDATE-STATE] Received request to update state for tppId: {}", inputSanify(tppId));

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        "Tpp not found during state update process")))
                .flatMap(tpp -> {
                    tpp.setState(state);
                    return tppRepository.save(tpp);
                })
                .map(mapperToDTO::map)
                .doOnSuccess(updatedTpp -> log.info("[TPP-SERVICE][UPDATE-STATE] State updated for tppId: {}", tppId))
                .doOnError(error -> log.error("[TPP-SERVICE][UPDATE-STATE] Error updating state for tppId {}: {}", tppId, error.getMessage()));
    }

    @Override
    public Mono<TppDTOWithoutTokenSection> getTppDetails(String tppId) {
        log.info("[TPP-SERVICE][GET] Received request to get TPP for tppId: {}", inputSanify(tppId));

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        "Tpp not found during get process")))
                .map(tppWithoutTokenSectionMapperToDTO::map)
                .doOnSuccess(tppDTO -> log.info("[TPP-SERVICE][GET] Found TPP with tppId: {}", tppId))
                .doOnError(error -> log.error("[TPP-SERVICE][GET] Error retrieving TPP for tppId {}: {}", tppId, error.getMessage()));
    }

    @Override
    public Mono<TokenSectionDTO> getTokenSection(String tppId) {
        log.info("[TPP-SERVICE][GET] Received request to get TokenSection for tppId: {}", inputSanify(tppId));

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        "Tpp not found during get process")))
                .map(Tpp::getTokenSection)
                .map(tokenSectionMapperToDTO::map)
                .doOnSuccess(tokenSectionDTO -> log.info("[TPP-SERVICE][GET] Found TokenSection for tppId: {}", tppId))
                .doOnError(error -> log.error("[TPP-SERVICE][GET] Error retrieving TokenSection for tppId {}: {}", tppId, error.getMessage()));
    }

}