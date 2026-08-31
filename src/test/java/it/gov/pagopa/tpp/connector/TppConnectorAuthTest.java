package it.gov.pagopa.tpp.connector;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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
        // Prepare response
        String jsonBody = "{\"access_token\": \"fake-token\", \"expires_in\": 3600}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jsonBody));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("scope", "openid");

        // Execute the test and verify the response
        StepVerifier.create(tppConnectorAuth.testConnection("/token", MediaType.APPLICATION_FORM_URLENCODED_VALUE, formData))
                .assertNext(response -> {
                    Assertions.assertEquals("SUCCESS", response.getStatus());
                    Assertions.assertEquals(200, response.getHttpStatus());
                    Assertions.assertTrue(response.getResponseTime() >= 0);
                    Assertions.assertNull(response.getResponseBody());
                })
                .verifyComplete();

        // Verify the request sent to the mock server
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertEquals("POST", recordedRequest.getMethod());
        Assertions.assertEquals("/token", recordedRequest.getPath());
        Assertions.assertTrue(recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE).contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE));
        
        // Verify that the request body contains the expected form data
        String sentBody = recordedRequest.getBody().readUtf8();
        Assertions.assertTrue(sentBody.contains("grant_type=client_credentials"));
        Assertions.assertTrue(sentBody.contains("scope=openid"));
    }

    @Test
    void testConnection_HttpError_ReturnsFailureDTO() throws InterruptedException {
        // Prepare response
        String errorBody = "{\"error\": \"unauthorized\", \"message\": \"invalid client credentials\"}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(errorBody));

        // Execute test and verify the response
        StepVerifier.create(tppConnectorAuth.testConnection("/token", MediaType.APPLICATION_JSON_VALUE, new LinkedMultiValueMap<>()))
                .assertNext(response -> {
                    Assertions.assertEquals("FAILURE", response.getStatus());
                    Assertions.assertEquals("HTTP_ERROR", response.getErrorType());
                    
                    Assertions.assertEquals(401, response.getHttpStatus());
                    
                    Assertions.assertNotNull(response.getDescription());
                    Assertions.assertTrue(response.getDescription().contains("Unauthorized"));

                    Assertions.assertNotNull(response.getResponseTime());
                    Assertions.assertTrue(response.getResponseTime() >= 0);

                    // Control the response body to ensure it contains the error details
                    Assertions.assertNotNull(response.getResponseBody());
                    Assertions.assertTrue(response.getResponseBody().contains("unauthorized"));
                })
                .verifyComplete();

        // Verify the request sent to the mock server
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        Assertions.assertEquals("POST", recordedRequest.getMethod());
        Assertions.assertEquals("/token", recordedRequest.getPath());
        Assertions.assertTrue(recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE).contains(MediaType.APPLICATION_JSON_VALUE));
    }
}