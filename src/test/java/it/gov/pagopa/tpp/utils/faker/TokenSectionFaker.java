package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.model.TokenSection;

import java.util.HashMap;

public class TokenSectionFaker {

    private TokenSectionFaker(){}

    public static TokenSection mockInstance() {

        HashMap<String, String> pathProps = new HashMap<>();
        pathProps.put("pathKey1", "test");

        HashMap<String, String> bodyProps = new HashMap<>();
        bodyProps.put("bodyKey1", "test");

        return TokenSection.builder()
                .contentType("application/json")
                .pathAdditionalProperties(pathProps)
                .bodyAdditionalProperties(bodyProps)
                .build();
    }
}
