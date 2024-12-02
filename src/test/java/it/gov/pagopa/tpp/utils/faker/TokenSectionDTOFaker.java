package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;

import java.util.HashMap;

public class TokenSectionDTOFaker {

    private TokenSectionDTOFaker() {}

    public static TokenSectionDTO mockInstance() {
        return TokenSectionDTO.builder()
                .contentType("application/json")
                .pathAdditionalProperties(new HashMap<>() {{
                    put("pathKey1", "test");
                }})
                .bodyAdditionalProperties(new HashMap<>() {{
                    put("bodyKey1", "test");
                }})
                .build();
    }
}
