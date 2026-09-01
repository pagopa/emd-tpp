package it.gov.pagopa.tpp.connector.tpp;

import org.springframework.util.MultiValueMap;

import it.gov.pagopa.tpp.dto.TppConnectionResponseDTO;
import reactor.core.publisher.Mono;

public interface TppConnectorAuth {
    
    /**
     * Performs a connectivity and authentication test towards an external TPP service.
     * 
     * <p>This method sends a POST request to the specified endpoint and captures the outcome.
     * Unlike standard connector methods, it is designed to catch exceptions (such as 4xx/5xx errors,
     * timeouts, or connection failures) and wrap them into a structured response object 
     * instead of propagating the error signal.</p>
     *
     * @param finalUrl    the full destination URL of the TPP's authorization endpoint
     * @param contentType the media type for the Request Header (e.g., "application/x-www-form-urlencoded")
     * @param formData    the payload containing authentication parameters (e.g., client credentials)
     * @return a {@link Mono} emitting a {@link TppConnectionResponseDTO} that encapsulates 
     *         the test result, including success or failure details.
     */
    Mono<TppConnectionResponseDTO> testConnection(String finalUrl, String contentType, MultiValueMap<String, String> formData);
    
}
