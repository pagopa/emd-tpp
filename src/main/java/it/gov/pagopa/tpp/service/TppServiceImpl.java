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

    private Mono<List<TppDTO>> checkMapForTppIds(List<String> tppIdList) {
        return Flux.fromIterable(tppIdList)
                .flatMap(tppId -> {
                    Tpp tpp = tppMapService.getFromMap(tppId);
                    if (tpp != null) {
                        log.info("[TPP-SERVICE][GET-ENABLED] Found TPP in MAP: {}", tpp.getTppId());
                        return Mono.just(mapperToDTO.map(tpp));
                    } else {
                        return Mono.empty();
                    }
                })
                .collectList();
    }

    private List<String> getMissingTppIds(List<String> tppIdList, List<TppDTO> cacheResult) {
        Set<String> cachedIds = cacheResult.stream()
                .map(TppDTO::getTppId)
                .collect(Collectors.toSet());
        return tppIdList.stream()
                .filter(tppId -> !cachedIds.contains(tppId))
                .toList();
    }


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
                    existingTpp.setPaymentButton(tppDTOWithoutTokenSection.getPaymentButton());
                    existingTpp.setAgentDeepLinks(tppDTOWithoutTokenSection.getAgentDeepLinks());
                    return tppRepository.save(existingTpp)
                            .map(tppWithoutTokenSectionMapperToDTO::map)
                            .doOnSuccess(savedTpp -> log.info("[TPP-SERVICE][UPSERT] Updated existing TPP with tppId: {}" ,savedTpp.getTppId()))
                            .doOnError(error -> log.error("[TPP-SERVICE][SAVE] Error saving TPP with tppId {}: {}" , existingTpp.getTppId(), error.getMessage()));
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
                    log.info("[TPP-SERVICE][UPDATE] Updating TokenSection for TPP with tppId: {}", existingTpp.getTppId());

                    TokenSection tokenSection = tokenSectionMapperToObject.map(tokenSectionDTO);
                    return azureKeyService.getKey(tppId)
                            .flatMap(keyVaultKey -> tokenSectionCryptService.keyEncrypt(tokenSection, keyVaultKey))
                            .flatMap(encryptionResult -> {
                                existingTpp.setLastUpdateDate(LocalDateTime.now());
                                existingTpp.setTokenSection(tokenSection);

                                return tppRepository.save(existingTpp)
                                        .map(tpp -> tokenSectionMapperToDTO.map(tpp.getTokenSection()))
                                        .doOnSuccess(updatedTokenSection -> log.info("[TPP-SERVICE][UPDATE] Updated TokenSection for tppId: {}", existingTpp.getTppId()))
                                        .doOnError(error -> log.error("[TPP-SERVICE][UPDATE] Error updating TokenSection for tppId {}: {}",  existingTpp.getTppId(), error.getMessage()));
                            });
                })
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED, ExceptionMessage.TPP_NOT_ONBOARDED)));
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
        return azureKeyService.createRsaKey(tppId)
                .flatMap(keyVaultKey -> tokenSectionCryptService.keyEncrypt(tppDTO.getTokenSection(), keyVaultKey))
                .flatMap(encryptionResult -> {
                    Tpp tppToSave = mapperToObject.map(tppDTO);
                    tppToSave.setTppId(tppId);
                    tppToSave.setLastUpdateDate(LocalDateTime.now());
                    tppToSave.setCreationDate(LocalDateTime.now());
                    return tppRepository.save(tppToSave)
                            .doOnSuccess(savedTpp -> log.info("[TPP-SERVICE][UPSERT] Created new TPP with tppId: {}", tppToSave.getTppId()))
                            .doOnError(error -> log.error("[TPP-SERVICE][SAVE] Error saving TPP with tppId {}: {}", tppToSave.getTppId(), error.getMessage()));
                });
    }


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
                .map(mapperToDTO::map)
                .doOnSuccess(updatedTpp -> log.info("[TPP-SERVICE][UPDATE-STATE] State updated for tppId: {}", updatedTpp.getTppId()))
                .doOnError(error -> log.error("[TPP-SERVICE][UPDATE-STATE] Error updating state for tppId {}: {}", tppId, error.getMessage()));
    }

    @Override
    public Mono<TppDTOWithoutTokenSection> getTppDetails(String tppId) {
        log.info("[TPP-SERVICE][GET] Received request to get TPP for tppId: {}", tppId);

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        TPP_NOT_FOUND)))
                .map(tppWithoutTokenSectionMapperToDTO::map)
                .doOnSuccess(tppDTO -> log.info("[TPP-SERVICE][GET] Found TPP with tppId: {}", tppDTO.getTppId()))
                .doOnError(error -> log.error("[TPP-SERVICE][GET] Error retrieving TPP for tppId {}: {}", tppId, error.getMessage()));
    }

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

    @Override
    public Mono<TokenSectionDTO> getTokenSection(String tppId) {
        log.info("[TPP-SERVICE][GET] Received request to get TokenSection for tppId: {}", tppId);

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        TPP_NOT_FOUND)))
                .flatMap(tpp -> {
                    TokenSection tokenSection = tpp.getTokenSection();
                    return tokenSectionCryptService.keyDecrypt(tpp.getTokenSection(), tpp.getTppId())
                            .map(decryptionResult -> tokenSectionMapperToDTO.map(tokenSection));
                })
                .doOnSuccess(tokenSectionDTO -> log.info("[TPP-SERVICE][GET] Found TokenSection for tppId: {}", tppId))
                .doOnError(error -> log.error("[TPP-SERVICE][GET] Error retrieving TokenSection for tppId {}: {}", tppId, error.getMessage()));
    }

    @Override
    public Mono<TppDTO> deleteTpp(String tppId){
        log.info("[TPP-SERVICE][DELETE] Received request to delete TPP for tppId: {}",tppId);

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        TPP_NOT_FOUND)))
                .flatMap(tpp -> tppRepository.delete(tpp).then(Mono.just(mapperToDTO.map(tpp))))
                .doOnSuccess(tokenSectionDTO -> log.info("[TPP-SERVICE][DELETE] Delete TPP for tppId: {}",tppId))
                .doOnError(error -> log.error("[TPP-SERVICE][DELETE] Error Delete TPP for tppId {}: {}",tppId, error.getMessage()));

    }


    @Override
    public Mono<NetworkResponseDTO> testConnection(String tppName) {
        return Mono.just(createNetworkResponse(tppName));
    }

    private NetworkResponseDTO createNetworkResponse(String tppName){
        NetworkResponseDTO networkResponseDTO = new NetworkResponseDTO();
        networkResponseDTO.setCode("PAGOPA_NETWORK_TEST");
        networkResponseDTO.setMessage(tppName+" ha raggiunto i nostri sistemi");
        return networkResponseDTO;
    }


}