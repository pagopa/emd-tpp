package it.gov.pagopa.tpp.dto.mapper;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.model.TokenSection;
import org.springframework.stereotype.Service;

@Service
public class TokenSectionObjectToDTOMapper {

    public TokenSectionDTO map(TokenSection tokenSection) {
        return TokenSectionDTO.builder()
                .contentType(tokenSection.getContentType())
                .pathAdditionalProperties(tokenSection.getPathAdditionalProperties())
                .bodyAdditionalProperties(tokenSection.getBodyAdditionalProperties())
                .build();

    }
}
