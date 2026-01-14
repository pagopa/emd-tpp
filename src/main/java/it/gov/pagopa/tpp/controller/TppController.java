package it.gov.pagopa.tpp.controller;

import it.gov.pagopa.tpp.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST Controller interface for managing TPP operations. 
 * It provides comprehensive CRUD operations with specialized endpoints
 * <p>
 * Base Path: {@code /emd/tpp}
 */
@Tag(
    name = "TPP Management", 
    description = "API per la gestione delle terze parti."
)
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
    @Operation(
        summary = "Get list of tpp",
        description = "Get list of tpp."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tpp list retrieved successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Tpp Id list")
    })
    @PostMapping("/list")
    Mono<ResponseEntity<List<TppDTO>>> getEnabledList(@Parameter(description = "Tpp list", required = true) @Valid @RequestBody TppIdList tppIdList);

    /**
     * Update the state of an existing TPP.
     *
     * @param tppUpdateState to update
     * @return a {@link Mono} containing a {@link ResponseEntity} with 
     *          the updated {@link TppDTO} if successful
     */
    @Operation(
        summary = "Update the state of an existing TPP.",
        description = "Update the state of an existing TPP."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tpp updated successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Tpp Id")
    })
    @PutMapping()
    Mono<ResponseEntity<TppDTO>> updateState(@Parameter(description = "Tpp state", required = true) @Valid @RequestBody TppUpdateState tppUpdateState);

    /**
     * Update the isPaymentEnabled of an existing TPP.
     *
     * @param tppId       of the TPP to update
     * @param tppUpdateIsPaymentEnabled to update
     * @return a {@link Mono} containing a {@link ResponseEntity} with 
     *         204 No Content if the update is successful
     */
    @Operation(
        summary = "Update the isPaymentEnabled of an existing TPP.",
        description = "Update the isPaymentEnabled of an existing TPP."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tpp updated successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Tpp Id")
    })
    @PutMapping("/{tppId}/payment-enabled")
    Mono<ResponseEntity<Void>> updateIsPaymentEnabled(@Parameter(description = "TPP identifier", example = "TPP_XYZ_123") @Valid @PathVariable String tppId, 
    @Parameter(description = "TPP isPaymentEnabled", example = "true") @Valid @RequestBody TppUpdateIsPaymentEnabled tppUpdateIsPaymentEnabled);

    /**
     * Creates and saves a new tpp
     *
     * @param tppDTO to save
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          the saved {@link TppDTO} if successful
     */
    @Operation(
        summary = "Creates and saves a new tpp.",
        description = "Creates and saves a new tpp."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tpp created successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Tpp Informations")
    })
    @PostMapping("/save")
    Mono<ResponseEntity<TppDTO>> save(@Valid @RequestBody TppDTO tppDTO);

    /**
     * Updates tpp excluding token section information
     *
     * @param tppDTOWithoutTokenSection tpp information excluding token section
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          the updated {@link TppDTOWithoutTokenSection} if successful
     */
    @Operation(
        summary = "Updates tpp excluding token section information.",
        description = "Updates tpp excluding token section information."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tpp updated successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters")
    })
    @PutMapping("/update")
    Mono<ResponseEntity<TppDTOWithoutTokenSection>> updateTppDetails(@Parameter(description = "TPP info") @Valid @RequestBody TppDTOWithoutTokenSection tppDTOWithoutTokenSection);

    /**
     * Update TokenSection of a specific TPP
     *
     * @param tppId       of the TPP to update
     * @param tokenSectionDTO updated token section
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          the updated {@link TokenSectionDTO} if successful
     */
    @Operation(
        summary = "Update TokenSection of a specific TPP.",
        description = "Update TokenSection of a specific TPP."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tpp updated successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters")
    })
    @PutMapping("/update/{tppId}/token")
    Mono<ResponseEntity<TokenSectionDTO>> updateTokenSection(@Parameter(description = "TPP identifier", example = "TPP_XYZ_123") @Valid @PathVariable String tppId, 
    @Parameter(description = "Token section info") @Valid @RequestBody TokenSectionDTO tokenSectionDTO);

    /**
     * Get a tpp (without token section)
     *
     * @param tppId to get
     * @return a {@link Mono} containing a {@link ResponseEntity} with 
     *          {@link TppDTOWithoutTokenSection} if found
     */
    @Operation(
        summary = "Get a tpp (without token section).",
        description = "Get a tpp (without token section)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tpp retrieved successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid TPP Id")
    })
    @GetMapping("/{tppId}")
    Mono<ResponseEntity<TppDTOWithoutTokenSection>> getTppDetails(@Parameter(description = "TPP identifier", example = "TPP_XYZ_123") @Valid @PathVariable String tppId);

    /**
     * Get TokenSection of a TPP
     *
     * @param tppId to get token section
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          {@link TokenSectionDTO} containing credentials if found
     */
    @Operation(
        summary = "Get TokenSection of a TPP.",
        description = "Get TokenSection of a TPP."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token section retrieved successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid TPP Id")
    })
    @GetMapping("/{tppId}/token")
    Mono<ResponseEntity<TokenSectionDTO>> getTokenSection(@Parameter(description = "TPP identifier", example = "TPP_XYZ_123") @Valid @PathVariable String tppId);

    /**
     * Get a tpp (without token section) by entity id
     *
     * @param entityId to get
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          {@link TppDTOWithoutTokenSection} if found
     */
    @Operation(
        summary = "Get a tpp (without token section).",
        description = "Get a tpp (without token section)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "TPP retrieved successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Entity Id")
    })
    @GetMapping("/entityId/{entityId}")
    Mono<ResponseEntity<TppDTOWithoutTokenSection>> getTppByEntityId(@Parameter(description = "Entity identifier", example = "86363574890") @Valid @PathVariable String entityId);

    /**
     * Tests the network connection to a specific TPP
     * @param tppName
     * @return a {@link Mono} containing {@link ResponseEntity} with {@link NetworkResponseDTO} containing connection test results
     */
    @Operation(
        summary = "Tests the network connection to a specific TPP.",
        description = "Tests the network connection to a specific TPP."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token section retrieved successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid Company Name")
    })
    @GetMapping("/network/connection/{tppName}")
    Mono<ResponseEntity<NetworkResponseDTO>> testConnection(@Parameter(description = "Company Name", example = "BancaX") @Valid @PathVariable String tppName);

    /**
     * Delete a tpp
     *
     * @param tppId to delete
     * @return a {@link Mono} containing a {@link ResponseEntity} with
     *          the deleted {@link TppDTO} if successful
     */
    @Operation(
        summary = "Delete a tpp.",
        description = "Delete a tpp."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "TPP deleted successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid TPP Id")
    })
    @DeleteMapping("/test/delete/{tppId}")
    Mono<ResponseEntity<TppDTO>> deleteTpp(@Parameter(description = "Entity identifier", example = "86363574890") @PathVariable String tppId);
}
