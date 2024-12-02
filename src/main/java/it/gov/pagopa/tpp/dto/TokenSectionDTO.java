package it.gov.pagopa.tpp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
public class TokenSectionDTO {
    private String contentType;
    private Map<String, String> pathAdditionalProperties;
    private Map<String, String> bodyAdditionalProperties;
}
