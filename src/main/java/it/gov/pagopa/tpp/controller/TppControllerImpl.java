package it.gov.pagopa.tpp.controller;

import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppIdList;
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
    public Mono<ResponseEntity<TppDTO>> updateState(TppDTO tppDTO) {
        return tppService.updateState(tppDTO.getTppId(),tppDTO.getState())
                .map(ResponseEntity::ok);

    }

    @Override
    public Mono<ResponseEntity<TppDTO>> upsert(TppDTO tppDTO) {
        return tppService.upsert(tppDTO)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TppDTO>> get(String entityId) {
        return tppService.get(entityId)
                .map(ResponseEntity::ok);
    }

}
