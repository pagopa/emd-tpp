package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TppService {

    Mono<List<TppDTO>> getEnabledList(List<String> tppIdList);

    Mono<TppDTO> createNewTpp(TppDTO tppDTO, String tppId);

    Mono<TppDTOWithoutTokenSection> updateTppDetails(TppDTOWithoutTokenSection tppDTOWithoutTokenSection);

    Mono<TokenSectionDTO> updateTokenSection(String tppId, TokenSectionDTO tokenSectionDTO);

    Mono<TppDTO> updateState(String tppId, Boolean state);

    Mono<TppDTOWithoutTokenSection> getTppDetails(String tppId);

    Mono<TokenSectionDTO> getTokenSection(String tppId);

    Mono<TppDTOWithoutTokenSection> getTppByEntityId(String entityId);

}
