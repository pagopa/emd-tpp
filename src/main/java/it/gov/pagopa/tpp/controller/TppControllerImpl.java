package it.gov.pagopa.tpp.controller;

import it.gov.pagopa.tpp.dto.*;
import it.gov.pagopa.tpp.service.TppServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static it.gov.pagopa.common.utils.Utils.inputSanitization;

/**
 * Implementation of the {@link TppController} interface.
 * <p>
 * All string inputs from path variables and request parameters are sanitized using
 * {@link Utils#inputSanitization(String)} to prevent log injection attacks and ensure secure logging.
 */
@RestController
public class TppControllerImpl implements TppController {

    private final TppServiceImpl tppService;

    public TppControllerImpl(TppServiceImpl tppService) {
        this.tppService = tppService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<List<TppDTO>>> getEnabledList(TppIdList tppIdList) {
        return tppService.getEnabledList(tppIdList.getIds())
                .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TppDTO>> updateState(TppUpdateState tppUpdateState) {
        return tppService.updateState(inputSanitization(tppUpdateState.getTppId()), tppUpdateState.getState())
                .map(ResponseEntity::ok);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TppDTO>> updateIsPaymentEnabled(TppUpdateIsPaymentEnabled tppUpdateIsPaymentEnabled) {
        return tppService.updateIsPaymentEnabled(inputSanitization(tppUpdateIsPaymentEnabled.getTppId()), tppUpdateIsPaymentEnabled.getIsPaymentEnabled())
                .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TppDTO>> save(TppDTO tppDTO) {
        return tppService.createNewTpp(tppDTO, String.format("%s-%d", UUID.randomUUID(), System.currentTimeMillis()))
                .map(ResponseEntity::ok);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TppDTOWithoutTokenSection>> updateTppDetails(TppDTOWithoutTokenSection tppDTOWithoutTokenSection) {
        return tppService.updateTppDetails(tppDTOWithoutTokenSection)
                .map(ResponseEntity::ok);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TokenSectionDTO>> updateTokenSection(String tppId, TokenSectionDTO tokenSectionDTO) {
        return tppService.updateTokenSection(inputSanitization(tppId), tokenSectionDTO)
                .map(ResponseEntity::ok);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TppDTOWithoutTokenSection>> getTppDetails(String tppId) {
        return tppService.getTppDetails(inputSanitization(tppId))
                .map(ResponseEntity::ok);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TokenSectionDTO>> getTokenSection(String tppId) {
        return tppService.getTokenSection(inputSanitization(tppId))
                .map(ResponseEntity::ok);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TppDTOWithoutTokenSection>> getTppByEntityId(String entityId) {
        return tppService.getTppByEntityId(inputSanitization(entityId))
                .map(ResponseEntity::ok);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<NetworkResponseDTO>> testConnection(String tppName) {
        return tppService.testConnection(inputSanitization(tppName))
                .map(ResponseEntity::ok);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ResponseEntity<TppDTO>> deleteTpp(String tppId) {
        return tppService.deleteTpp(inputSanitization(tppId))
                .map(ResponseEntity::ok);
    }
}
