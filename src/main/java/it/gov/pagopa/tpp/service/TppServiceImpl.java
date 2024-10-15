package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.constants.OnboardingTppConstants.ExceptionName;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.mapper.TppObjectToDTOMapper;
import it.gov.pagopa.tpp.configuration.ExceptionMap;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.model.mapper.TppDTOToObjectMapper;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


import java.util.List;

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
        log.info("[EMD][TPP][GET-ENABLED] Received tppIdList: {}",(tppIdList));
        return tppRepository.findByTppIdInAndStateTrue(tppIdList)
                .collectList()
                .map(tppList -> tppList.stream()
                        .map(mapperToDTO::map)
                        .toList()
                )
                .doOnSuccess(tppDTOList -> log.info("[EMD][TPP][GET-ENABLED] Tpps founded:  {}",(tppDTOList.size())))
                .doOnError(error -> log.error("[EMD][TPP][GET-ENABLED] Error:  {}", error.getMessage()));
        }




    @Override
    public Mono<TppDTO> upsert(TppDTO tppDTO) {
        log.info("[EMD][TPP][UPSERT] Received tppDTO:  {}", inputSanify(tppDTO.toString()));
        Tpp tppReceived = mapperToObject.map(tppDTO);

        return tppRepository.findByTppId(tppDTO.getEntityId())
                .flatMap(tppDB -> {
                    log.info("[EMD][TPP][UPSERT] TPP with entityId:[{}] already exists",(tppDTO.getEntityId()));
                    return tppRepository.save(tppReceived)
                            .map(mapperToDTO::map) // Map to DTO after saving
                            .doOnSuccess(savedTpp -> log.info("[EMD][TPP][UPSERT] Updated existing TPP"))
                            .doOnError(error -> log.error("[EMD][TPP][UPSERT] Error:  {}", error.getMessage()));
                })
                .switchIfEmpty(
                        tppRepository.save(tppReceived)
                                .map(mapperToDTO::map)
                                .doOnSuccess(savedTpp -> log.info("[EMD][TPP][UPSERT] Created TPP"))
                                .doOnError(error -> log.error("[EMD][TPP][UPSERT] Error:  {}",error.getMessage()))
                );
    }

    @Override
    public Mono<TppDTO> updateState(String tppId, Boolean state) {
        log.info("[EMD][UPDATE-TPP] Received tppId:  {}",inputSanify(tppId));
        return tppRepository.findByTppId(tppId)
                .switchIfEmpty(Mono.error(exceptionMap.getException(ExceptionName.TPP_NOT_ONBOARDED)))
                .flatMap(tpp -> {
                    tpp.setState(state);
                    return tppRepository.save(tpp);
                })
                .map(mapperToDTO::map)
                .doOnSuccess(updatedTpp -> log.info("[EMD][TPP][UPDATE-STATE] Updated"))
                .doOnError(error -> log.error("[EMD][TPP][UPDATE-STATE] Error:  {}",error.getMessage()));
    }


    @Override
    public Mono<TppDTO> get(String entityId) {
        log.info("[EMD][UPDATE-TPP] Received tppId:  {}",inputSanify(entityId));
        return tppRepository.findByTppId(entityId)
                .switchIfEmpty(Mono.error(exceptionMap.getException(ExceptionName.TPP_NOT_ONBOARDED)))
                .map(mapperToDTO::map)
                .doOnSuccess(updatedTpp -> log.info("[EMD][TPP][GET] Founded"))
                .doOnError(error -> log.error("[EMD][TPP][GET] Error:  {}",error.getMessage()));
    }
}
