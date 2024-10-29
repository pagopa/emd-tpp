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

    public TppServiceImpl(TppRepository tppRepository, TppObjectToDTOMapper mapperToDTO, TppDTOToObjectMapper mapperToObject, ExceptionMap exceptionMap) {
        this.tppRepository = tppRepository;
        this.mapperToDTO = mapperToDTO;
        this.mapperToObject = mapperToObject;
        this.exceptionMap = exceptionMap;
    }

    @Override
    public Mono<List<TppDTO>> getEnabledList(List<String> tppIdList) {
        log.info("[EMD-TPP][GET-ENABLED] Received tppIdList: {}",(tppIdList));
        return tppRepository.findByTppIdInAndStateTrue(tppIdList)
                .collectList()
                .map(tppList -> tppList.stream()
                        .map(mapperToDTO::map)
                        .toList()
                )
                .doOnSuccess(tppDTOList -> log.info("[EMD-TPP][GET-ENABLED] Tpps founded:  {}",tppDTOList))
                .doOnError(error -> log.error("[EMD-TPP][GET-ENABLED] Error:  {}", error.getMessage()));
        }


    @Override
    public Mono<TppDTO> upsert(TppDTO tppDTO) {
        log.info("[EMD-TPP][UPSERT] Received tppDTO:  {}", inputSanify(tppDTO.toString()));
        Tpp tppReceived = mapperToObject.map(tppDTO);
        return tppRepository.findByTppId(tppDTO.getTppId())
                .flatMap(tppDB -> {
                    log.info("[EMD-TPP][UPSERT] TPP with tppId:[{}] already exists",(tppDTO.getTppId()));
                    tppReceived.setId(tppDB.getId());
                    tppReceived.setLastUpdateDate(LocalDateTime.now());
                    return tppRepository.save(tppReceived)
                            .map(mapperToDTO::map)
                            .doOnSuccess(savedTpp -> log.info("[EMD-TPP][UPSERT] Updated existing TPP"));
                })
                .switchIfEmpty(
                        Mono.defer(() -> {
                            tppReceived.setTppId("%s_%d".formatted(UUID.randomUUID().toString(), System.currentTimeMillis()));
                            tppReceived.setCreationDate(LocalDateTime.now());
                            tppReceived.setLastUpdateDate(LocalDateTime.now());
                            return tppRepository.save(tppReceived)
                                    .map(mapperToDTO::map)
                                    .doOnSuccess(savedTpp -> log.info("[EMD-TPP][UPSERT] Created TPP"));
                        })
                );
    }

    @Override
    public Mono<TppDTO> updateState(String tppId, Boolean state) {
        log.info("[EMD-TPP][UPDATE] Received tppId:  {}",inputSanify(tppId));
        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap
                        .throwException(ExceptionName.TPP_NOT_ONBOARDED,"Tpp not founded during update state process")))
                .flatMap(tpp -> {
                    tpp.setState(state);
                    return tppRepository.save(tpp);
                })
                .map(mapperToDTO::map)
                .doOnSuccess(updatedTpp -> log.info("[EMD][TPP][UPDATE-STATE] Updated"));
    }


    @Override
    public Mono<TppDTO> get(String tppId) {
        log.info("[EMD-TPP][GET] Received tppId:  {}",inputSanify(tppId));
        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap
                        .throwException(ExceptionName.TPP_NOT_ONBOARDED,"Tpp not founded during get process")))
                .map(mapperToDTO::map)
                .doOnSuccess(updatedTpp -> log.info("[EMD][TPP][GET] Founded"));
    }
}
