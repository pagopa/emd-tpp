package it.gov.pagopa.tpp.model.mapper;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.model.TokenSection;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for mapping {@link TokenSectionDTO} 
 * data transfer objects to {@link TokenSection} domain objects.
 */
@Service
public class TokenSectionDTOToObjectMapper {

    /**
     * Maps a {@link TokenSectionDTO} to its corresponding {@link TokenSection} domain object.
     * 
     * @param tokenSectionDTO the DTO containing token section data to be mapped
     * @return a new {@link TokenSection} domain object containing all mapped properties
     *         from the input DTO
     */
    public TokenSection map(TokenSectionDTO tokenSectionDTO) {
        return new TokenSection(
                tokenSectionDTO.getContentType(),
                tokenSectionDTO.getPathAdditionalProperties(),
                tokenSectionDTO.getBodyAdditionalProperties()
        );
    }
}
