package it.gov.pagopa.tpp.connector;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import it.gov.pagopa.tpp.connector.tpp.TppConnectorAuthImpl;
import reactor.test.StepVerifier;

import java.io.IOException;

class TppConnectorAuthTest {

    private static MockWebServer mockWebServer;
    private TppConnectorAuthImpl tppConnectorAuth;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        WebClient.Builder webClientBuilder = WebClient.builder().baseUrl(baseUrl);
        tppConnectorAuth = new TppConnectorAuthImpl(webClientBuilder);
    }

    @Test
    void testConnection_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"access_token\": \"fake-token\", \"expires_in\": 3600}"));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        StepVerifier.create(tppConnectorAuth.testConnection("/token", MediaType.APPLICATION_FORM_URLENCODED_VALUE, formData))
                .expectNextMatches(response -> 
                    response.get("access_token").equals("fake-token") &&
                    response.get("expires_in").equals(3600)
                )
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String contentType = recordedRequest.getHeader("Content-Type");

        Assertions.assertNotNull(contentType);
        Assertions.assertTrue(contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE));
        Assertions.assertEquals("POST", recordedRequest.getMethod());
        Assertions.assertEquals("/token", recordedRequest.getPath());
        Assertions.assertTrue(recordedRequest.getHeader("Content-Type").contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE));
    }

    @Test
    void testConnection_Error() throws InterruptedException  {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\": \"unauthorized\"}"));

        StepVerifier.create(tppConnectorAuth.testConnection("/token", MediaType.APPLICATION_JSON_VALUE, new LinkedMultiValueMap<>()))
                .expectError() // Verifichiamo che WebClient propaghi l'errore HTTP
                .verify();
        mockWebServer.takeRequest();
    }
}