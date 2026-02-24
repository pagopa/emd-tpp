package it.gov.pagopa.tpp.controller;

import it.gov.pagopa.tpp.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST Controller interface for managing TPP operations. 
 * It provides comprehensive CRUD operations with specialized endpoints
 * <p>
 * Base Path: {@code /emd/tpp}
 */
@RestController
@RequestMapping("/emd/tpp")
public interface TppController {

    /**
     * Get list of tpp based on the provided tpp ids.
     *
     * @param tppIdList whose data is to be retrieved
     * @return a {@link Mono} containing a {@link ResponseEntity} with 
     *          a list of {@link TppDTO} objects for found enabled TPPs
     */
    @PostMapping("/list")
    Mono<ResponseEntity<List<TppDTO>>> getEnabledList(@Valid @RequestBody TppIdList tppIdList);

    /**
     * Update the state of an existing TPP.
     *
     * @param tppUpdateState to update
     * @return a {@link Mono} containing a {@link ResponseEntity} with 
     *          the updated {@link TppDTO} if successful
     */
    @PutMapping()
    Mono<ResponseEntity<TppDTO>> updateState(@Valid @RequestBody TppUpdateState tppUpdateState);

    /**
     * Update the isPaymentEnabled of an existing TPP.
     *
     * @param tppId       of the TPP to update
     * @param tppUpdateIsPaymentEnabled to update
     * @return a {@link Mono} containing a {@link ResponseEntity} with 
     *         204 No Content if the update is successful
     */
    @PutMapping("/{tppId}/payment-enabled")
    Mono<ResponseEntity<Void>> updateIsPaymentEnabled(@Valid @PathVariable String tppId, @Valid @RequestBody TppUpdateIsPaymentEnabled tppUpdateIsPaymentEnabled);

    /**
     * Creates and saves a new tpp
     *
     * @param tppDTO to save
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          the saved {@link TppDTO} if successful
     */
    @PostMapping("/save")
    Mono<ResponseEntity<TppDTO>> save(@Valid @RequestBody TppDTO tppDTO);

    /**
     * Updates tpp excluding token section information
     *
     * @param tppDTOWithoutTokenSection tpp information excluding token section
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          the updated {@link TppDTOWithoutTokenSection} if successful
     */
    @PutMapping("/update")
    Mono<ResponseEntity<TppDTOWithoutTokenSection>> updateTppDetails(@Valid @RequestBody TppDTOWithoutTokenSection tppDTOWithoutTokenSection);

    /**
     * Update TokenSection of a specific TPP
     *
     * @param tppId       of the TPP to update
     * @param tokenSectionDTO updated token section
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          the updated {@link TokenSectionDTO} if successful
     */
    @PutMapping("/update/{tppId}/token")
    Mono<ResponseEntity<TokenSectionDTO>> updateTokenSection(@Valid @PathVariable String tppId, @Valid @RequestBody TokenSectionDTO tokenSectionDTO);

    /**
     * Get a tpp (without token section)
     *
     * @param tppId to get
     * @return a {@link Mono} containing a {@link ResponseEntity} with 
     *          {@link TppDTOWithoutTokenSection} if found
     */
    @GetMapping("/{tppId}")
    Mono<ResponseEntity<TppDTOWithoutTokenSection>> getTppDetails(@Valid @PathVariable String tppId);

    /**
     * Get TokenSection of a TPP
     *
     * @param tppId to get token section
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          {@link TokenSectionDTO} containing credentials if found
     */
    @GetMapping("/{tppId}/token")
    Mono<ResponseEntity<TokenSectionDTO>> getTokenSection(@Valid @PathVariable String tppId);

    /**
     * Get a tpp (without token section) by entity id
     *
     * @param entityId to get
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          {@link TppDTOWithoutTokenSection} if found
     */
    @GetMapping("/entityId/{entityId}")
    Mono<ResponseEntity<TppDTOWithoutTokenSection>> getTppByEntityId(@Valid @PathVariable String entityId);

    /**
     * Tests the network connection to a specific TPP
     * @param tppName
     * @return a {@link Mono} containing {@link ResponseEntity} with {@link NetworkResponseDTO} containing connection test results
     */
    @GetMapping("/network/connection/{tppName}")
    Mono<ResponseEntity<NetworkResponseDTO>> testConnection(@Valid @PathVariable String tppName);

    /**
     * Delete a tpp
     *
     * @param tppId to delete
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          the deleted {@link TppDTO} if successful
     */
    @DeleteMapping("/test/delete/{tppId}")
    Mono<ResponseEntity<TppDTO>> deleteTpp(@PathVariable String tppId);

    /**
     * Get all whitelists.
     * Path: GET /emd/tpp/whitelist
     *
     * @return a {@link Mono} containing a {@link ResponseEntity} with the map of TPP IDs and their whitelists
     */
    @GetMapping("/whitelist")
    Mono<ResponseEntity<Map<String, List<String>>>> getWhitelists();

    /**
     * Get whitelist for a specific TPP.
     * Path: GET /emd/tpp/{tppId}/whitelist
     *
     * @param tppId the TPP identifier
     * @return a {@link Mono} containing a {@link ResponseEntity} with the list of recipient IDs
     */
    @GetMapping("/{tppId}/whitelist")
    Mono<ResponseEntity<List<String>>> getWhitelistByTppId(@PathVariable String tppId);

    /**
     * Add a recipient to a TPP's whitelist.
     * Path: POST /emd/tpp/{tppId}/whitelist
     * Body: { "recipientId": "string" }
     *
     * @param tppId the TPP identifier
     * @param whitelistRecipientDTO valid recipient information
     * @return a {@link Mono} containing a {@link ResponseEntity} with status 204
     */
    @PostMapping("/{tppId}/whitelist")
    Mono<ResponseEntity<Void>> addRecipientToWhitelist(@PathVariable String tppId, @Valid @RequestBody WhitelistRecipientDTO whitelistRecipientDTO);

    /**
     * Remove a recipient from a TPP's whitelist.
     * Path: DELETE /emd/tpp/{tppId}/whitelist/{recipientId}
     *
     * @param tppId the TPP identifier
     * @param recipientId the recipient identifier to remove
     * @return a {@link Mono} containing a {@link ResponseEntity} with status 204
     */
    @DeleteMapping("/{tppId}/whitelist/{recipientId}")
    Mono<ResponseEntity<Void>> removeRecipientFromWhitelist(@PathVariable String tppId, @PathVariable String recipientId);

    /**
     * Update the entire whitelist for a specific TPP.
     * Path: PUT /emd/tpp/{tppId}/whitelist
     * Body: [ "id1", "id2" ]
     *
     * @param tppId the TPP identifier
     * @param recipientIds the new list of recipient IDs
     * @return a {@link Mono} containing a {@link ResponseEntity} with status 204
     */
    @PutMapping("/{tppId}/whitelist")
    Mono<ResponseEntity<Void>> updateWhitelist(@PathVariable String tppId, @RequestBody List<String> recipientIds);

}
