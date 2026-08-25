package it.gov.pagopa.tpp.connector.tpp;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class TppConnectorAuthImpl implements TppConnectorAuth {

    private final WebClient webClient;

    public TppConnectorAuthImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Map<String, Object>> testConnection(String finalUrl, String contentType, MultiValueMap<String, String> formData) {
        log.info("[TPP-CONNECTOR] Calling external Auth API");

        return webClient.post()
                .uri(finalUrl)
                .contentType(MediaType.valueOf(contentType))
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnError(e -> log.error("[TPP-CONNECTOR] Error: {}", e.getMessage()));
    }

}