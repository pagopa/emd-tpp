package it.gov.pagopa.tpp.controller;

import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppIdList;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
@RestController
@RequestMapping("/emd/tpp")
public interface TppController {

    /**
     * Get list of tpp
     *
     * @param tppIdList whose data is to be retrieved
     * @return outcome of retrieving the tpp
     */
    @PostMapping("/list")
    Mono<ResponseEntity<List<TppDTO>>> getEnabledList(@Valid @RequestBody TppIdList tppIdList);


    /**
     * Update a tpp
     *
     * @param tppDTO to update
     * @return outcome of the update
     */
    @PutMapping()
    Mono<ResponseEntity<TppDTO>> updateState(@Valid @RequestBody TppDTO tppDTO);

    /**
     * Save a tpp
     *
     * @param tppDTO to save
     * @return outcome of saving the tpp
     */
    @PostMapping()
    Mono<ResponseEntity<TppDTO>> upsert(@Valid @RequestBody TppDTO tppDTO);

    /**
     * Get a tpp
     *
     * @param tppId to get
     * @return  outcome of getting tpp
     */
    @GetMapping("/{tppId}")
    Mono<ResponseEntity<TppDTO>> get(@Valid @PathVariable String tppId);

}
