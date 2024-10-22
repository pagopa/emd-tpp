package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.dto.TppDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TppService {

    Mono<List<TppDTO>> getEnabledList(List<String> tppIdList);

    Mono<TppDTO> upsert(TppDTO tppDTO);

    Mono<TppDTO> updateState(String tppId, Boolean state);

    Mono<TppDTO> get(String tppId);
}
