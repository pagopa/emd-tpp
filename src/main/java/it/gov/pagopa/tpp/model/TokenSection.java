package it.gov.pagopa.tpp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

import java.util.Map;

@Data
@NoArgsConstructor
public class TokenSection {
    private MediaType contentType;
    private Map<String, String> pathAdditionalProperties;
    private Map<String, String> bodyAdditionalProperties;
}