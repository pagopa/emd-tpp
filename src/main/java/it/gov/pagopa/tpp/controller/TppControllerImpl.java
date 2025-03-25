package it.gov.pagopa.tpp.controller;

import it.gov.pagopa.tpp.dto.*;
import it.gov.pagopa.tpp.service.TppServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
public class TppControllerImpl implements TppController {

    private final TppServiceImpl tppService;

    public TppControllerImpl(TppServiceImpl tppService) {
        this.tppService = tppService;
    }

    @Override
    public Mono<ResponseEntity<List<TppDTO>>> getEnabledList(TppIdList tppIdList) {
        return tppService.getEnabledList(tppIdList.getIds())
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TppDTO>> updateState(TppUpdateState tppUpdateState) {
        return tppService.updateState(tppUpdateState.getTppId(), tppUpdateState.getState())
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TppDTO>> save(TppDTO tppDTO) {
        return tppService.createNewTpp(tppDTO, String.format("%s-%d", UUID.randomUUID(), System.currentTimeMillis()))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TppDTOWithoutTokenSection>> updateTppDetails(TppDTOWithoutTokenSection tppDTOWithoutTokenSection) {
        return tppService.updateTppDetails(tppDTOWithoutTokenSection)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TokenSectionDTO>> updateTokenSection(String tppId, TokenSectionDTO tokenSectionDTO) {
        return tppService.updateTokenSection(tppId, tokenSectionDTO)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TppDTOWithoutTokenSection>> getTppDetails(String tppId) {
        return tppService.getTppDetails(tppId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TokenSectionDTO>> getTokenSection(String tppId) {
        return tppService.getTokenSection(tppId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TppDTOWithoutTokenSection>> getTppByEntityId(String entityId) {
        return tppService.getTppByEntityId(entityId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<NetworkResponseDTO>> testConnection(String tppName) {
        return tppService.testConnection(tppName)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TppDTO>> deleteTpp(String tppId) {
        return tppService.deleteTpp(tppId)
                .map(ResponseEntity::ok);
    }


  
}
