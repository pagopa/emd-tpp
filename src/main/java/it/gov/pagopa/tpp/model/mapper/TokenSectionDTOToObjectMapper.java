package it.gov.pagopa.tpp.model.mapper;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.model.TokenSection;
import org.springframework.stereotype.Service;

@Service
public class TokenSectionDTOToObjectMapper {

    public TokenSection map(TokenSectionDTO tokenSectionDTO) {
        return new TokenSection(
                tokenSectionDTO.getContentType(),
                tokenSectionDTO.getPathAdditionalProperties(),
                tokenSectionDTO.getBodyAdditionalProperties()
        );
    }
}
