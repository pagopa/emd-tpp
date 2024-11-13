package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.configuration.ExceptionMap;
import it.gov.pagopa.tpp.constants.TppConstants.ExceptionName;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.mapper.TppObjectToDTOMapper;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.model.mapper.TppDTOToObjectMapper;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static it.gov.pagopa.common.utils.Utils.inputSanify;


@Service
@Slf4j
public class TppServiceImpl implements TppService {

    private final TppRepository tppRepository;
    private final TppObjectToDTOMapper mapperToDTO;
    private final TppDTOToObjectMapper mapperToObject;
    private final ExceptionMap exceptionMap;

    public TppServiceImpl(TppRepository tppRepository, TppObjectToDTOMapper mapperToDTO,
                          TppDTOToObjectMapper mapperToObject, ExceptionMap exceptionMap) {
        this.tppRepository = tppRepository;
        this.mapperToDTO = mapperToDTO;
        this.mapperToObject = mapperToObject;
        this.exceptionMap = exceptionMap;
    }

    @Override
    public Mono<List<TppDTO>> getEnabledList(List<String> tppIdList) {
        log.info("[TPP-SERVICE][GET-ENABLED] Received tppIdList: {}", tppIdList);

        return tppRepository.findByTppIdInAndStateTrue(tppIdList)
                .collectList()
                .map(tppList -> tppList.stream()
                        .map(mapperToDTO::map)
                        .toList())
                .doOnSuccess(tppDTOList -> log.info("[TPP-SERVICE][GET-ENABLED] Found TPPs: {}", tppDTOList))
                .doOnError(error -> log.error("[TPP-SERVICE][GET-ENABLED] Error retrieving enabled TPPs: {}", error.getMessage(), error));
    }

    @Override
    public Mono<TppDTO> upsert(TppDTO tppDTO) {
        log.info("[TPP-SERVICE][UPSERT] Received tppDTO: {}", inputSanify(tppDTO.toString()));

        if (tppDTO.getTppId() == null) {
            return tppRepository.findByEntityId(tppDTO.getEntityId())
                    .flatMap(existingTpp -> Mono.error(
                            exceptionMap.throwException(ExceptionName.TPP_ALREADY_ONBOARDED, "TPP already onboarded with entity ID.")
                    ))
                    .flatMap(entityNotFound -> {
                        Tpp tppToSave = mapperToObject.map(tppDTO);
                        tppToSave.setId(generateTppId());
                        return saveTpp(tppToSave, "[TPP-SERVICE][UPSERT] Created new TPP with tppId");
                    });
        } else {
            return tppRepository.findByTppId(tppDTO.getTppId())
                    .flatMap(existingTpp -> {
                        log.info("[TPP-SERVICE][UPSERT] TPP with tppId [{}] found. Updating...", tppDTO.getTppId());
                        Tpp tppToUpdate = mapperToObject.map(tppDTO);
                        tppToUpdate.setId(existingTpp.getId());
                        tppToUpdate.setLastUpdateDate(LocalDateTime.now());
                        return saveTpp(tppToUpdate, "[TPP-SERVICE][UPSERT] Updated existing TPP with tppId");
                    })
                    .switchIfEmpty(
                            Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED, "TPP not found for update."))
                    );
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
    public Mono<TppDTO> get(String tppId) {
        log.info("[TPP-SERVICE][GET] Received request to get TPP for tppId: {}", inputSanify(tppId));

        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        "Tpp not found during get process")))
                .map(mapperToDTO::map)
                .doOnSuccess(tppDTO -> log.info("[TPP-SERVICE][GET] Found TPP with tppId: {}", tppId))
                .doOnError(error -> log.error("[TPP-SERVICE][GET] Error retrieving TPP for tppId {}: {}", tppId, error.getMessage()));
    }

    private Mono<TppDTO> saveTpp(Tpp tppToSave, String successLogMessage) {
        return tppRepository.save(tppToSave)
                .map(mapperToDTO::map)
                .doOnSuccess(savedTpp -> log.info("{}: {}", successLogMessage, tppToSave.getTppId()))
                .doOnError(error -> log.error("[TPP-SERVICE][SAVE] Error saving TPP with tppId {}: {}", tppToSave.getTppId(), error.getMessage()));
    }

    private String generateTppId() {
        String newTppId = String.format("%s_%d", UUID.randomUUID(), System.currentTimeMillis());
        log.debug("[TPP-SERVICE][GENERATE-TPP-ID] Generated new TPP ID: {}", newTppId);
        return newTppId;
    }

}