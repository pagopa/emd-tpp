package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.dto.NetworkResponseDTO;
import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service interface for managing TPP operations.
 */
public interface TppService {

    /**
     * Retrieves a list of enabled TPP entities by their identifiers.
     * 
     * @param tppIdList the list of TPP identifiers to retrieve
     * @return a {@link Mono} containing a list of enabled {@link TppDTO} entities
     */
    Mono<List<TppDTO>> getEnabledList(List<String> tppIdList);

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

}
