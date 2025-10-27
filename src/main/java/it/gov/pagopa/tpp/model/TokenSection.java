package it.gov.pagopa.tpp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Represents a token section. It serves as a data model for storing token section information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TokenSection {
    private String contentType;
    private Map<String, String> pathAdditionalProperties;
    private Map<String, String> bodyAdditionalProperties;
}