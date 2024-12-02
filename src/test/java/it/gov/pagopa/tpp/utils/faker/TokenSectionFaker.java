package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.model.TokenSection;

import java.util.HashMap;

public class TokenSectionFaker {

    private TokenSectionFaker(){}

    public static TokenSection mockInstance() {
        return TokenSection.builder()
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
