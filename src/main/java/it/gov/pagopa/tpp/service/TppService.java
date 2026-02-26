package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.constants.TppConstants.ExceptionName;
import it.gov.pagopa.tpp.dto.NetworkResponseDTO;
import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import java.util.Map;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service interface for managing TPP operations.
 */
public interface TppService {

    /**
     * Retrieves a list of eenabled tpp or tpp with whitelistRecipient field containing the recipientId.
     * 
     * @param tppIdList the list of TPP identifiers to retrieve
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
     * Retrieves all whitelist recipientIds grouped by tppId.
     *
     * @return a {@link Mono} containing a map of tppId -> list of recipientIds.
     *         Returns an empty map if no whitelist is configured at system level.
     */
    Mono<Map<String, List<String>>> getAllWhitelistRecipientId();

    /**
     * Retrieves the whitelist recipientIds for a specific TPP.
     *
     * @param tppId the TPP identifier
     * @return a {@link Mono} containing the flat list of recipientIds on whitelist.
     *         Returns an empty list if the TPP exists but has no whitelist entries.
     * @throws TPP_NOT_ONBOARDED if the TPP does not exist
     */
    Mono<List<String>> getTppWhitelistRecipientId(String tppId);

    /**
     * Adds a single recipientId to the whitelist of a specific TPP.
     *
     * @param tppId       the TPP identifier
     * @param recipientId the recipientId to add
     * @return a {@link Mono} that completes empty when the insertion is successful
     * @throws TPP_NOT_ONBOARDED         if the TPP does not exist
     * @throws RECIPIENT_ALREADY_PRESENT if the recipientId is already in the whitelist
     */
    Mono<TppDTO> insertRecipientIdOnWhitelist(String tppId, String recipientId);

    /**
     * Removes a single recipientId from the whitelist of a specific TPP.
     *
     * @param tppId       the TPP identifier
     * @param recipientId the recipientId to remove
     * @return a {@link Mono} that completes empty when the removal is successful
     * @throws TPP_NOT_ONBOARDED   if the TPP does not exist
     * @throws RECIPIENT_NOT_FOUND if the recipientId is not present in the whitelist
     */
    Mono<TppDTO> removeRecipientIdOnWhitelist(String tppId, String recipientId);

    /**
     * Replaces the entire whitelist of a specific TPP with the provided list.
     * Passing an empty list will clear the whitelist.
     *
     * @param tppId        the TPP identifier
     * @param recipientIds the new list of recipientIds to set (can be empty)
     * @return a {@link Mono} that completes empty when the update is successful
     * @throws TPP_NOT_ONBOARDED if the TPP does not exist
     */
    Mono<TppDTO> updateRecipientIdOnWhitelist(String tppId, List<String> recipientIds);
}
