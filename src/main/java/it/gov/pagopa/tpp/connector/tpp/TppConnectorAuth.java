package it.gov.pagopa.tpp.connector.tpp;

import java.util.Map;

import org.springframework.util.MultiValueMap;

import reactor.core.publisher.Mono;

public interface TppConnectorAuth {
    
    Mono<Map<String, Object>> testConnection(String finalUrl, String contentType, MultiValueMap<String, String> formData);
    
}
