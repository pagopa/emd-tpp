package it.gov.pagopa.tpp.connector.tpp;

import java.util.Map;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public interface TppConnectorAuth {
    
    /**
     * Executes an external POST request to a TPP's authentication service.
     *
     * @param finalUrl    the complete URL of the TPP authentication endpoint
     * @param contentType the value for the Content-Type header
     * @param formData    the multi-value map containing the request body parameters
     * @return a {@link Mono} containing the TPP's response deserialized into a {@link Map}
     */
    Mono<Map<String, Object>> testConnection(String finalUrl, String contentType, MultiValueMap<String, String> formData);
    
}
