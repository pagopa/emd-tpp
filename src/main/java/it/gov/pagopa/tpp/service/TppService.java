package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import reactor.core.publisher.Mono;

public interface TppService {

    Mono<TppDTO> getEnabled(String tppId);

    Mono<TppDTO> createNewTpp(TppDTO tppDTO, String tppId);

    Mono<TppDTOWithoutTokenSection> updateTppDetails(TppDTOWithoutTokenSection tppDTOWithoutTokenSection);

    Mono<TokenSectionDTO> updateTokenSection(String tppId, TokenSectionDTO tokenSectionDTO);

    Mono<TppDTO> updateState(String tppId, Boolean state);

    Mono<TppDTOWithoutTokenSection> getTppDetails(String tppId);

    Mono<TokenSectionDTO> getTokenSection(String tppId);

}
