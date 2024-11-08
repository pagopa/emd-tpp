package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.constants.TppConstants.ExceptionName;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.mapper.TppObjectToDTOMapper;
import it.gov.pagopa.tpp.configuration.ExceptionMap;
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
        log.info("[EMD-TPP][GET-ENABLED] Received tppIdList: {}", tppIdList);
        return tppRepository.findByTppIdInAndStateTrue(tppIdList)
                .collectList()
                .map(tppList -> tppList.stream()
                        .map(mapperToDTO::map)
                        .toList())
                .doOnSuccess(tppDTOList -> log.info("[EMD-TPP][GET-ENABLED] Found TPPs: {}", tppDTOList))
                .doOnError(error -> log.error("[EMD-TPP][GET-ENABLED] Error: {}", error.getMessage()));
    }

    @Override
    public Mono<TppDTO> upsert(TppDTO tppDTO) {
        log.info("[EMD-TPP][UPSERT] Received tppDTO: {}", inputSanify(tppDTO.toString()));
        Tpp tppToSave = mapperToObject.map(tppDTO);

        return tppRepository.findByTppId(tppDTO.getTppId())
                .flatMap(existingTpp -> {
                    log.info("[EMD-TPP][UPSERT] TPP with tppId [{}] already exists", tppDTO.getTppId());
                    tppToSave.setId(existingTpp.getId());
                    tppToSave.setLastUpdateDate(LocalDateTime.now());
                    return saveTpp(tppToSave, "[EMD-TPP][UPSERT] Updated existing TPP ");
                })
                .switchIfEmpty(Mono.defer(() -> {
                    tppToSave.setTppId(generateTppId());
                    tppToSave.setCreationDate(LocalDateTime.now());
                    tppToSave.setLastUpdateDate(LocalDateTime.now());
                    return saveTpp(tppToSave, "[EMD-TPP][UPSERT] Created new TPP");
                }));
    }

    @Override
    public Mono<TppDTO> updateState(String tppId, Boolean state) {
        log.info("[EMD-TPP][UPDATE-STATE] Received tppId: {}", inputSanify(tppId));
        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        "Tpp not found during state update process")))
                .flatMap(tpp -> {
                    tpp.setState(state);
                    return tppRepository.save(tpp);
                })
                .map(mapperToDTO::map)
                .doOnSuccess(updatedTpp -> log.info("[EMD-TPP][UPDATE-STATE] State updated for tppId: {}", tppId));
    }

    @Override
    public Mono<TppDTO> get(String tppId) {
        log.info("[EMD-TPP][GET] Received tppId: {}", inputSanify(tppId));
        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(ExceptionName.TPP_NOT_ONBOARDED,
                        "Tpp not found during get process")))
                .map(mapperToDTO::map)
                .doOnSuccess(tppDTO -> log.info("[EMD-TPP][GET] Found TPP with tppId: {}", tppId));
    }

       private Mono<TppDTO> saveTpp(Tpp tppToSave, String successLogMessage) {
        return tppRepository.save(tppToSave)
                .map(mapperToDTO::map)
                .doOnSuccess(savedTpp -> log.info(successLogMessage));
    }

    private String generateTppId() {
        return String.format("%s_%d", UUID.randomUUID(), System.currentTimeMillis());
    }
}