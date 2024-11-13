package it.gov.pagopa.tpp.controller;

import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppIdList;
import it.gov.pagopa.tpp.dto.TppUpdateState;
import it.gov.pagopa.tpp.service.TppServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

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
        return tppService.updateState(tppUpdateState.getTppId(),tppUpdateState.getState())
                .map(ResponseEntity::ok);

    }

    @Override
    public Mono<ResponseEntity<TppDTO>> save(TppDTO tppDTO) {
        return tppService.createNewTpp(tppDTO)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TppDTO>> update(TppDTO tppDTO) {
        return tppService.updateExistingTpp(tppDTO)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TppDTO>> get(String tppId) {
        return tppService.get(tppId)
                .map(ResponseEntity::ok);
    }

}
