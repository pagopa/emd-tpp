package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing TPP operations.
 */
public interface TppService {

    /**
     * Retrieves a list of enabled TPP entities by their identifiers.
     * 
     * @param tppIdList the list of TPP identifiers to retrieve
     * @param recipientId the recipient identifier to filter by whitelist
     * @return a {@link Mono} containing a list of enabled {@link TppDTO} entities
     */
    Mono<List<TppDTO>> filterEnabledList(List<String> tppIdList, String recipientId);

    /**
     * Creates a new TPP entity with the specified configuration.
     * 
     * @param tppDTO the TPP data transfer object containing TPP details
     * @param tppId the unique identifier for the new TPP
     * @return a {@link Mono} containing the created {@link TppDTO}
     */
    Mono<TppDTO> createNewTpp(TppDTO tppDTO, String tppId);

    /**
     * Updates TPP details excluding the token section.
     * 
     * @param tppDTOWithoutTokenSection the TPP data transfer object without token section
     * @return a Mo{@link Mono}no containing the updated {@link TppDTOWithoutTokenSection}
     */
    Mono<TppDTOWithoutTokenSection> updateTppDetails(TppDTOWithoutTokenSection tppDTOWithoutTokenSection);

    /**
     * Updates the token section configuration for a specific TPP.
     * 
     * @param tppId the TPP identifier
     * @param tokenSectionDTO the token section configuration to update
     * @return a {@link Mono} containing the updated {@link TokenSectionDTO}
     */
    Mono<TokenSectionDTO> updateTokenSection(String tppId, TokenSectionDTO tokenSectionDTO);

    /**
     * Updates the state of a specific TPP.
     * 
     * @param tppId the TPP identifier
     * @param state the new state to set (true for enabled, false for disabled)
     * @return a {@link Mono} containing the updated {@link TppDTO}
     */
    Mono<TppDTO> updateState(String tppId, Boolean state);

    /**
     * Updates the isPaymentEnabled of a specific TPP.
     * 
     * @param tppId the TPP identifier
     * @param isPaymentEnabled the new isPaymentEnabled to set (true for enabled, false for disabled)
     * @return a {@link Mono} containing the updated {@link TppDTO}
     */
    Mono<TppDTO> updateIsPaymentEnabled(String tppId, Boolean isPaymentEnabled);


    /**
     * Retrieves TPP details excluding the token section.
     * 
     * @param tppId the TPP identifier
     * @return a {@link Mono} containing the {@link TppDTOWithoutTokenSection}
     */
    Mono<TppDTOWithoutTokenSection> getTppDetails(String tppId);

    /**
     * Retrieves the token section configuration for a specific TPP.
     * 
     * @param tppId the TPP identifier
     * @return a {@link Mono} containing the {@link TokenSectionDTO}
     */
    Mono<TokenSectionDTO> getTokenSection(String tppId);

    /**
     * Retrieves TPP details by entity identifier.
     * 
     * @param entityId the entity identifier
     * @return a {@link Mono} containing the {@link TppDTOWithoutTokenSection}
     */
    Mono<TppDTOWithoutTokenSection> getTppByEntityId(String entityId);

    /**
     * Tests the network connectivity to a specific TPP.
     * 
     * @param tppName the name of the TPP to test connectivity
     * @return a {@link Mono} containing the {@link NetworkResponseDTO} with test results
     */
    Mono<NetworkResponseDTO> testConnection(String tppName);

    /**
     * Deletes a TPP entity by its identifier.
     * 
     * @param tppId the TPP identifier to delete
     * @return a {@link Mono} containing the deleted {@link TppDTO}
     */
    Mono<TppDTO> deleteTpp(String tppId);

    /**
     * Retrieves the whitelist of all TPPs.
     *
     * @return a {@link Mono} containing a map of TPP IDs to their whitelists
     */
    Mono<Map<String, List<String>>> getAllWhitelists();

    /**
     * Retrieves the whitelist of a specific TPP.
     *
     * @param tppId the TPP identifier
     * @return a {@link Mono} containing the list of whitelisted recipient IDs
     */
    Mono<List<String>> getWhitelistByTppId(String tppId);

    /**
     * Adds a recipient ID to a TPP's whitelist.
     *
     * @param tppId the TPP identifier
     * @param recipientId the recipient identifier to add
     * @return a {@link Mono} signaling completion
     */
    Mono<Void> addRecipientToWhitelist(String tppId, String recipientId);

    /**
     * Removes a recipient ID from a TPP's whitelist.
     *
     * @param tppId the TPP identifier
     * @param recipientId the recipient identifier to remove
     * @return a {@link Mono} signaling completion
     */
    Mono<Void> removeRecipientFromWhitelist(String tppId, String recipientId);

    /**
     * Replaces a TPP's whitelist with a new list of recipient IDs.
     *
     * @param tppId the TPP identifier
     * @param recipientIds the new list of recipient identifiers
     * @return a {@link Mono} signaling completion
     */
    Mono<Void> updateWhitelist(String tppId, List<String> recipientIds);

}
