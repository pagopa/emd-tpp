package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.configuration.ExceptionMap;
import it.gov.pagopa.tpp.constants.TppConstants.ExceptionMessage;
import it.gov.pagopa.tpp.constants.TppConstants.ExceptionName;
import it.gov.pagopa.tpp.dto.NetworkResponseDTO;
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
import it.gov.pagopa.tpp.service.keyvault.AzureKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the TppService interface providing comprehensive TPP management functionality.
 * 
 * This service class implements all TPP-related operations including creation, retrieval, updating,
 * and deletion. It integrates with multiple components including
 * database repository, caching service, cryptographic services, and Azure Key Vault for secure
 * token management.
 * 
 */
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
    private final TppMapService tppMapService;
    private final TokenSectionCryptService tokenSectionCryptService;
    private final AzureKeyService azureKeyService;
    private static final String TPP_NOT_FOUND = "Tpp not found during get process";

    public TppServiceImpl(TppRepository tppRepository, TppObjectToDTOMapper mapperToDTO, TppWithoutTokenSectionObjectToDTOMapper tppWithoutTokenSectionMapperToDTO, TokenSectionObjectToDTOMapper tokenSectionMapperToDTO,
                          TppDTOToObjectMapper mapperToObject, TokenSectionDTOToObjectMapper tokenSectionMapperToObject, ExceptionMap exceptionMap, AzureKeyService azureKeyService, TppMapService tppMapService, TokenSectionCryptService tokenSectionCryptService) {
        this.tppRepository = tppRepository;
        this.mapperToDTO = mapperToDTO;
        this.tppWithoutTokenSectionMapperToDTO = tppWithoutTokenSectionMapperToDTO;
        this.tokenSectionMapperToDTO = tokenSectionMapperToDTO;
        this.mapperToObject = mapperToObject;
        this.tokenSectionMapperToObject = tokenSectionMapperToObject;
        this.exceptionMap = exceptionMap;
        this.tppMapService = tppMapService;
        this.tokenSectionCryptService = tokenSectionCryptService;
        this.azureKeyService = azureKeyService;
    }


    /**
     * {@inheritDoc}
     *
     * <p>
     * This method first checks the cache for requested TPP IDs, then fetches any missing
     * entries from the database. Missing entries are automatically cached after retrieval
     * with their token sections decrypted for immediate use.
     */
    @Override
    public Mono<List<TppDTO>> getEnabledList(List<String> tppIdList) {
        log.info("[TPP-SERVICE][GET-ENABLED] Received tppIdList: {}", tppIdList);

        return checkMapForTppIds(tppIdList)
                .flatMap(cacheResult -> {
                    List<String> missingTppIds = getMissingTppIds(tppIdList, cacheResult);
                    if (missingTppIds.isEmpty()) {
                        return Mono.just(cacheResult);
                    }
                    log.info("[TPP-SERVICE][GET-ENABLED] TPPs not in cache: {}",missingTppIds);
                    return tppRepository.findByTppIdInAndStateTrue(missingTppIds)
                            .flatMap(tpp -> tokenSectionCryptService.keyDecrypt(tpp.getTokenSection(), tpp.getTppId())
                                    .flatMap(decryptionResult -> tppMapService.addToMap(tpp).map(cachingResult -> mapperToDTO.map(tpp)))
                            )
                            .collectList()
                            .flatMap(tppDTOList -> {
                                cacheResult.addAll(tppDTOList);
                                return Mono.just(cacheResult);
                            });
                })
                .doOnSuccess(tppDTOList -> log.info("[TPP-SERVICE][GET-ENABLED] Retrival ended"))
                .doOnError(error -> log.error("[TPP-SERVICE][GET-ENABLED] Error retrieving enabled TPPs: {}", error.getMessage()));
    }

    /**
     * Checks the cache for TPP entities corresponding to the provided IDs.
     * 
     * @param tppIdList the list of TPP identifiers to check in cache
     * @return a {@link Mono} containing a list of cached {@link TppDTO} entities
     */
    private Mono<List<TppDTO>> checkMapForTppIds(List<String> tppIdList) {
        return Flux.fromIterable(tppIdList)
            .flatMap(tppId -> tppMapService.getFromMap(tppId)
                    .doOnNext(tpp -> log.info("[TPP-SERVICE][GET-ENABLED] Found TPP in MAP: {}", tpp.getTppId()))
                    .map(mapperToDTO::map)
            )
            .collectList();
    }

    /**
     * Identifies TPP IDs that are not present in the cache result.
     * 
     * @param tppIdList the original list of requested TPP IDs
     * @param cacheResult the list of TPP DTOs found in cache
     * @return a list of TPP IDs that were not found in cache
     */
    private List<String> getMissingTppIds(List<String> tppIdList, List<TppDTO> cacheResult) {
        Set<String> cachedIds = cacheResult.stream()
                .map(TppDTO::getTppId)
                .collect(Collectors.toSet());
        return tppIdList.stream()
                .filter(tppId -> !cachedIds.contains(tppId))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<TppDTOWithoutTokenSection> updateTppDetails(TppDTOWithoutTokenSection tppDTOWithoutTokenSection) {
        if (tppDTOWithoutTokenSection.getTppId() == null)
            return Mono.error(exceptionMap.throwException(ExceptionName.GENERIC_ERROR,
                    ExceptionMessage.GENERIC_ERROR));

        return tppRepository.findByTppId(tppDTOWithoutTokenSection.getTppId())
                .flatMap(existingTpp -> {
                    log.info("[TPP-SERVICE][UPSERT] TPP with tppId {} already exists. Updating...", existingTpp.getTppId());
                    existingTpp.setLastUpdateDate(LocalDateTime.now());
                    existingTpp.setMessageUrl(tppDTOWithoutTokenSection.getMessageUrl());
                    existingTpp.setAuthenticationUrl(tppDTOWithoutTokenSection.getAuthenticationUrl());
                    existingTpp.setContact(tppDTOWithoutTokenSection.getContact());
                    existingTpp.setBusinessName(tppDTOWithoutTokenSection.getBusinessName());
                    existingTpp.setLegalAddress(tppDTOWithoutTokenSection.getLegalAddress());
                    existingTpp.setPspDenomination(tppDTOWithoutTokenSection.getPspDenomination());
                    existingTpp.setAgentDeepLinks(tppDTOWithoutTokenSection.getAgentDeepLinks());
                    existingTpp.setMessageTemplate(tppDTOWithoutTokenSection.getMessageTemplate());
                    existingTpp.setIsPaymentEnabled(tppDTOWithoutTokenSection.getIsPaymentEnabled());
                    return tppRepository.save(existingTpp)
                            .flatMap(savedTpp -> tppMapService.addToMap(savedTpp).thenReturn(savedTpp))
                            .map(tppWithoutTokenSectionMapperToDTO::map)
                            .doOnSuccess(savedTpp -> log.info("[TPP-SERVICE][UPSERT] Updated existing TPP with tppId: {}" ,savedTpp.getTppId()))
                            .doOnError(error -> log.error("[TPP-SERVICE][SAVE] Error saving TPP with tppId {}: {}" , existingTpp.getTppId(), error.getMessage()));
                })
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        ExceptionMessage.TPP_NOT_ONBOARDED)));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Encrypts the new token section data using Azure Key Vault before storing
     * it in the database. The encryption uses the TPP's specific RSA key.
     */
    @Override
    public Mono<TokenSectionDTO> updateTokenSection(String tppId, TokenSectionDTO tokenSectionDTO) {
        if (tppId == null)
            return Mono.error(exceptionMap.throwException(ExceptionName.GENERIC_ERROR,
                    ExceptionMessage.GENERIC_ERROR));

        return tppRepository.findByTppId(tppId)
                .flatMap(existingTpp -> {
                    log.info("[TPP-SERVICE][UPDATE] Updating TokenSection for TPP with tppId: {}", existingTpp.getTppId());

                    TokenSection tokenSection = tokenSectionMapperToObject.map(tokenSectionDTO);
                    return azureKeyService.getKey(tppId)
                            .flatMap(keyVaultKey -> tokenSectionCryptService.keyEncrypt(tokenSection, keyVaultKey))
                            .flatMap(encryptionResult -> {
                                existingTpp.setLastUpdateDate(LocalDateTime.now());
                                existingTpp.setTokenSection(tokenSection);

                                return tppRepository.save(existingTpp)
                                        .flatMap(savedTpp -> tppMapService.addToMap(savedTpp).thenReturn(savedTpp))
                                        .map(tpp -> tokenSectionMapperToDTO.map(tpp.getTokenSection()))
                                        .doOnSuccess(updatedTokenSection -> log.info("[TPP-SERVICE][UPDATE] Updated TokenSection for tppId: {}", existingTpp.getTppId()))
                                        .doOnError(error -> log.error("[TPP-SERVICE][UPDATE] Error updating TokenSection for tppId {}: {}",  existingTpp.getTppId(), error.getMessage()));
                            });
                })
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED, ExceptionMessage.TPP_NOT_ONBOARDED)));
    }

    /**
     * {@inheritDoc}
     *
     * Creates a new TPP entity with encrypted token section and Azure Key Vault key generation.
     * <p>
     * This method creates a new RSA key in Azure Key Vault, encrypts the token section,
     * and saves the TPP entity to the database. It includes duplicate detection based
     * on entity ID to prevent multiple registrations.
     */
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

    /**
     * Creates and saves a new TPP entity with Azure Key Vault integration.
     * 
     * @param tppDTO the TPP data for creation
     * @param tppId the TPP identifier
     * @return a {@link Mono} containing the created {@link Tpp} entity
     */
    private Mono<Tpp> createAndSaveNewTpp(TppDTO tppDTO, String tppId) {
        log.info("[TPP-SERVICE][UPSERT] Creating new entry with generated tppId: {}", tppId);
        return azureKeyService.createRsaKey(tppId)
                .flatMap(keyVaultKey -> tokenSectionCryptService.keyEncrypt(tppDTO.getTokenSection(), keyVaultKey))
                .flatMap(encryptionResult -> {
                    Tpp tppToSave = mapperToObject.map(tppDTO);
                    tppToSave.setTppId(tppId);
                    tppToSave.setLastUpdateDate(LocalDateTime.now());
                    tppToSave.setCreationDate(LocalDateTime.now());
                    return tppRepository.save(tppToSave)
                            .flatMap(savedTpp -> tppMapService.addToMap(savedTpp).thenReturn(savedTpp))
                            .doOnSuccess(savedTpp -> log.info("[TPP-SERVICE][UPSERT] Created new TPP with tppId: {}", tppToSave.getTppId()))
                            .doOnError(error -> log.error("[TPP-SERVICE][SAVE] Error saving TPP with tppId {}: {}", tppToSave.getTppId(), error.getMessage()));
                });
    }

    /**
     * {@inheritDoc}
     *
     * The operation also updates the last modification timestamp.
     */
    @Override
    public Mono<TppDTO> updateState(String tppId, Boolean state) {
        log.info("[TPP-SERVICE][UPDATE-STATE] Received request to update state for tppId: {}", tppId);

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        "Tpp not found during state update process")))
                .flatMap(tpp -> {
                    tpp.setState(state);
                    tpp.setLastUpdateDate(LocalDateTime.now());
                    return tppRepository.save(tpp);
                })
                .flatMap(savedTpp -> tppMapService.addToMap(savedTpp).thenReturn(savedTpp))
                .map(mapperToDTO::map)
                .doOnSuccess(updatedTpp -> log.info("[TPP-SERVICE][UPDATE-STATE] State updated for tppId: {}", updatedTpp.getTppId()))
                .doOnError(error -> log.error("[TPP-SERVICE][UPDATE-STATE] Error updating state for tppId {}: {}", tppId, error.getMessage()));
    }

    /**
     * {@inheritDoc}
     *
     * The operation also updates the last modification timestamp.
     */
    @Override
    public Mono<TppDTO> updateIsPaymentEnabled(String tppId, Boolean isPaymentEnabled) {
        log.info("[TPP-SERVICE][UPDATE-IS-PAYMENT-ENABLED] Received request to update isPaymentEnabled for tppId: {}", tppId);

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        "Tpp not found during isPaymentEnabled update process")))
                .flatMap(tpp -> {
                    tpp.setIsPaymentEnabled(isPaymentEnabled);
                    tpp.setLastUpdateDate(LocalDateTime.now());
                    return tppRepository.save(tpp);
                })
                .flatMap(savedTpp -> tppMapService.addToMap(savedTpp).thenReturn(savedTpp))
                .map(mapperToDTO::map)
                .doOnSuccess(updatedTpp -> log.info("[TPP-SERVICE][UPDATE-IS-PAYMENT-ENABLED] isPaymentEnabled updated for tppId: {}", updatedTpp.getTppId()))
                .doOnError(error -> log.error("[TPP-SERVICE][UPDATE-IS-PAYMENT-ENABLED] Error updating isPaymentEnabled for tppId {}: {}", tppId, error.getMessage()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<TppDTOWithoutTokenSection> getTppDetails(String tppId) {
        log.info("[TPP-SERVICE][GET] Received request to get TPP for tppId: {}", tppId);

        return tppMapService.getFromMap(tppId)
            .map(tpp -> {
                log.info("[TPP-SERVICE][GET] Found TPP in MAP for tppId: {}", tppId);
                return tppWithoutTokenSectionMapperToDTO.map(tpp);
            })
            .switchIfEmpty(Mono.defer(() ->
                tppRepository.findByTppId(tppId)
                    .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED, TPP_NOT_FOUND)))
                    .flatMap(dbTpp -> tppMapService.addToMap(dbTpp).thenReturn(dbTpp))
                    .map(tppWithoutTokenSectionMapperToDTO::map)
            ))
            .doOnSuccess(tppDTO -> log.info("[TPP-SERVICE][GET] Found TPP with tppId: {}", tppDTO.getTppId()))
            .doOnError(error -> log.error("[TPP-SERVICE][GET] Error retrieving TPP for tppId {}: {}", tppId, error.getMessage()));
    }

    /**
     * {@inheritDoc}
     *
     * This method allows TPP lookup using the entity's fiscal code rather than
     * the internal TPP identifier. Returns TPP information without sensitive
     * token section data.
     * 
     * @param entityId the fiscal code/entity identifier of the TPP
     * @return a {@link Mono} containing the {@link TppDTOWithoutTokenSection}
     * @throws TPP_NOT_ONBOARDED if the TPP is not found in the database
     */
    @Override
    public Mono<TppDTOWithoutTokenSection> getTppByEntityId(String entityId) {
        log.info("[TPP-SERVICE][GET] Received request to get TPP for entityId: {}",  entityId);

        return tppRepository.findByEntityId(entityId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        TPP_NOT_FOUND)))
                .map(tppWithoutTokenSectionMapperToDTO::map)
                .doOnSuccess(tppDTO -> log.info("[TPP-SERVICE][GET] Found TPP with entityId: {}",tppDTO.getEntityId()))
                .doOnError(error -> log.error("[TPP-SERVICE][GET] Error retrieving TPP for entityId {}: {}", entityId, error.getMessage()));
    }

    /**
     * {@inheritDoc}
     * 
     * This method fetches the TPP's token section from the database, decrypts
     * the sensitive authentication data using Azure Key Vault, and returns
     * the decrypted configuration.
     */
    @Override
    public Mono<TokenSectionDTO> getTokenSection(String tppId) {
        log.info("[TPP-SERVICE][GET] Received request to get TokenSection for tppId: {}", tppId);

        return tppMapService.getFromMap(tppId)
            .map(tpp -> {
                log.info("[TPP-SERVICE][GET] Found TokenSection in MAP for tppId: {}", tppId);
                return tokenSectionMapperToDTO.map(tpp.getTokenSection());
            })
            .switchIfEmpty(Mono.defer(() ->
                tppRepository.findByTppId(tppId)
                    .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        TPP_NOT_FOUND)))
                    .flatMap(dbTpp -> tppMapService.addToMap(dbTpp).thenReturn(dbTpp))
                    .map(tpp -> tokenSectionMapperToDTO.map(tpp.getTokenSection()))
            ))
            .doOnSuccess(tokenSectionDTO -> log.info("[TPP-SERVICE][GET] Found TokenSection for tppId: {}", tppId))
            .doOnError(error -> log.error("[TPP-SERVICE][GET] Error retrieving TokenSection for tppId {}: {}", tppId, error.getMessage()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<TppDTO> deleteTpp(String tppId){
        log.info("[TPP-SERVICE][DELETE] Received request to delete TPP for tppId: {}",tppId);

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        TPP_NOT_FOUND)))
                .flatMap(tpp -> tppRepository.delete(tpp)
                    .then(Mono.fromRunnable(() -> tppMapService.removeFromMap(tppId)))
                    .thenReturn(mapperToDTO.map(tpp))
                )
                .doOnSuccess(tokenSectionDTO -> log.info("[TPP-SERVICE][DELETE] Delete TPP for tppId: {}",tppId))
                .doOnError(error -> log.error("[TPP-SERVICE][DELETE] Error Delete TPP for tppId {}: {}",tppId, error.getMessage()));

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<NetworkResponseDTO> testConnection(String tppName) {
        return Mono.just(createNetworkResponse(tppName));
    }

    /**
     * Creates a standardized network response for connectivity testing.
     * 
     * @param tppName the name of the TPP for the response message
     * @return a {@link NetworkResponseDTO} containing the test response
     */
    private NetworkResponseDTO createNetworkResponse(String tppName){
        NetworkResponseDTO networkResponseDTO = new NetworkResponseDTO();
        networkResponseDTO.setCode("PAGOPA_NETWORK_TEST");
        networkResponseDTO.setMessage(tppName+" ha raggiunto i nostri sistemi");
        return networkResponseDTO;
    }


}