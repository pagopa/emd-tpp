package it.gov.pagopa.tpp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class TokenSection {
    private String contentType;
    private Map<String, String> pathAdditionalProperties;
    private Map<String, String> bodyAdditionalProperties;
}