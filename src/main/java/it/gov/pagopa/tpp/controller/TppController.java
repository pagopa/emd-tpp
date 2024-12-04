package it.gov.pagopa.tpp.controller;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.dto.TppUpdateState;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/emd/tpp")
public interface TppController {

    /**
     * Get enabled tpp
     *
     * @param tppId whose data is to be retrieved
     * @return outcome of retrieving the tpp
     */
    @GetMapping("/{tppId}/enabled")
    Mono<ResponseEntity<TppDTO>> getEnabled(@Valid @PathVariable String tppId);

    /**
     * Update a tpp
     *
     * @param tppUpdateState to update
     * @return outcome of the update
     */
    @PutMapping()
    Mono<ResponseEntity<TppDTO>> updateState(@Valid @RequestBody TppUpdateState tppUpdateState);

    /**
     * Save a tpp
     *
     * @param tppDTO to save
     * @return outcome of saving the tpp
     */
    @PostMapping("/save")
    Mono<ResponseEntity<TppDTO>> save(@Valid @RequestBody TppDTO tppDTO);


    @PutMapping("/update")
    Mono<ResponseEntity<TppDTOWithoutTokenSection>> updateTppDetails(@Valid @RequestBody TppDTOWithoutTokenSection tppDTOWithoutTokenSection);

    /**
     * Update TokenSection of a TPP
     *
     * @param tppId       of the TPP to update
     * @param tokenSectionDTO updated token section
     * @return outcome of the update
     */
    @PutMapping("/update/{tppId}/token")
    Mono<ResponseEntity<TokenSectionDTO>> updateTokenSection(@Valid @PathVariable String tppId, @Valid @RequestBody TokenSectionDTO tokenSectionDTO);

    /**
     * Get a tpp (without token section)
     *
     * @param tppId to get
     * @return outcome of getting tpp
     */
    @GetMapping("/{tppId}")
    Mono<ResponseEntity<TppDTOWithoutTokenSection>> getTppDetails(@Valid @PathVariable String tppId);

    /**
     * Get TokenSection of a TPP
     *
     * @param tppId to get token section
     * @return outcome of getting token section
     */
    @GetMapping("/{tppId}/token")
    Mono<ResponseEntity<TokenSectionDTO>> getTokenSection(@Valid @PathVariable String tppId);

}
