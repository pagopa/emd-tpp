package it.gov.pagopa.tpp.dto.mapper;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.model.TokenSection;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for mapping {@link TokenSection} domain objects to 
 * {@link TokenSectionDTO} data transfer objects.
 */
@Service
public class TokenSectionObjectToDTOMapper {
    /**
     * Maps a {@link TokenSection} domain object to its corresponding
     * {@link TokenSectionDTO} representation.
     * 
     * @param tokenSection the domain object containing token section data to be mapped
     * @return a new {@link TokenSectionDTO} instance containing all mapped properties
     */ 
    public TokenSectionDTO map(TokenSection tokenSection) {
        return TokenSectionDTO.builder()
                .contentType(tokenSection.getContentType())
                .pathAdditionalProperties(tokenSection.getPathAdditionalProperties())
                .bodyAdditionalProperties(tokenSection.getBodyAdditionalProperties())
                .build();

    }
}
