package it.gov.pagopa.tpp.connector.tpp;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import it.gov.pagopa.tpp.dto.TppConnectionResponseDTO;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class TppConnectorAuthImpl implements TppConnectorAuth {

    private final WebClient webClient;

    public TppConnectorAuthImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation uses {@link WebClient} and calculates the response time.
     * It enforces a <strong>10-second timeout</strong> and maps failures as follows:
     * <ul>
     *   <li>{@code SUCCESS}: Server responded with 2xx.</li>
     *   <li>{@code HTTP_ERROR}: Server responded with non-2xx (e.g., 401, 404, 500).</li>
     *   <li>{@code TIMEOUT}: Request exceeded the 10s limit.</li>
     *   <li>{@code NETWORK_ERROR}: DNS, SSL, or connection issues.</li>
     * </ul>
     * </p>
     */
    @Override
    public Mono<TppConnectionResponseDTO> testConnection(String finalUrl, String contentType, MultiValueMap<String, String> formData) {
        //Used to measure the latency of the request
        long startTime = System.currentTimeMillis();
        log.info("[TPP-CONNECTOR] Calling external Auth API");

        return webClient.post()
                .uri(finalUrl)
                .contentType(MediaType.valueOf(contentType))
                .bodyValue(formData)
                .retrieve()
                //Get the entire response as a Map to capture any additional metadata returned by the TPP server
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
                //Manage successful responses and map them to TppConnectionResponseDTO
                .<TppConnectionResponseDTO>map(response -> TppConnectionResponseDTO.builder()
                        .status("SUCCESS")
                        .httpStatus(response.getStatusCode().value())
                        .responseTime(System.currentTimeMillis() - startTime)
                        .description("Connection established successfully")
                        .responseBody(response.getBody() != null ? response.getBody().toString() : null)
                        .build())
                .timeout(Duration.ofSeconds(10))
                //Manage errors and map them to TppConnectionResponseDTO with appropriate errorType and description
                .onErrorResume(e -> {
                    long duration = System.currentTimeMillis() - startTime;
                    TppConnectionResponseDTO.TppConnectionResponseDTOBuilder builder = TppConnectionResponseDTO.builder()
                            .status("FAILURE")
                            .responseTime(duration);

                    if (e instanceof WebClientResponseException we) {
                        builder.errorType("HTTP_ERROR")
                            .httpStatus(we.getStatusCode().value())
                            .description("External server returned an error: " + we.getStatusText())
                            .responseBody(we.getResponseBodyAsString());
                    } else if (e instanceof TimeoutException) {
                        builder.errorType("TIMEOUT")
                            .description("Request timed out after 10 seconds");
                    } else {
                        builder.errorType("NETWORK_ERROR")
                            .description(e.getMessage());
                    }

                    return Mono.just(builder.build());
                });
    }

}