package it.gov.pagopa.tpp.connector;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.TimeUnit;

class TppConnectorAuthTest {

    private MockWebServer mockWebServer;
    private TppConnectorAuthImpl tppConnectorAuth;

    @BeforeEach
    void setUp() throws IOException {
        // New mock server for each test to ensure isolation
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder().baseUrl(baseUrl);
        tppConnectorAuth = new TppConnectorAuthImpl(webClientBuilder);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
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
                    Assertions.assertNotNull(response.getResponseBody());
                    Assertions.assertTrue(response.getResponseBody().contains("fake-token"));
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

    @Test
    void testConnection_InvalidContentType_ReturnsConfigError() {
        // Test try-catch
        StepVerifier.create(tppConnectorAuth.testConnection("/token", "formato-non-valido", new LinkedMultiValueMap<>()))
                .assertNext(response -> {
                    Assertions.assertEquals("FAILURE", response.getStatus());
                    Assertions.assertEquals("CONFIG_ERROR", response.getErrorType());
                    Assertions.assertTrue(response.getDescription().contains("Invalid content type"));
                    Assertions.assertTrue(response.getResponseTime() >= 0);
                })
                .verifyComplete();
    }

    @Test
    void testConnection_Timeout_ReturnsTimeoutError() {
        // Simulate a delayed response to trigger a timeout
        mockWebServer.enqueue(new MockResponse()
                .setBodyDelay(11, TimeUnit.SECONDS) 
                .setBody("{\"status\": \"late\"}")
                .setResponseCode(200));

        StepVerifier.create(tppConnectorAuth.testConnection("/token", MediaType.APPLICATION_JSON_VALUE, new LinkedMultiValueMap<>()))
                .assertNext(response -> {
                    Assertions.assertEquals("FAILURE", response.getStatus());
                    Assertions.assertEquals("TIMEOUT", response.getErrorType());
                    Assertions.assertTrue(response.getDescription().contains("timed out"));
                })
                .verifyComplete();
    }

    @Test
    void testConnection_NetworkError_ReturnsNetworkError() {
        // Simulate a sudden socket disconnection to force a network error
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        StepVerifier.create(tppConnectorAuth.testConnection("/token", MediaType.APPLICATION_JSON_VALUE, new LinkedMultiValueMap<>()))
                .assertNext(response -> {
                    Assertions.assertEquals("FAILURE", response.getStatus());
                    Assertions.assertEquals("NETWORK_ERROR", response.getErrorType());
                    Assertions.assertNotNull(response.getDescription());
                })
                .verifyComplete();
    }

    @Test
    void testConnection_InternalServerError_ReturnsHttpError() {
        // Test error response from the server (500 Internal Server Error)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        StepVerifier.create(tppConnectorAuth.testConnection("/token", MediaType.APPLICATION_JSON_VALUE, new LinkedMultiValueMap<>()))
                .assertNext(response -> {
                    Assertions.assertEquals("FAILURE", response.getStatus());
                    Assertions.assertEquals("HTTP_ERROR", response.getErrorType());
                    Assertions.assertEquals(500, response.getHttpStatus());
                })
                .verifyComplete();
    }

    @Test
    void testConnection_SuccessWithEmptyBody() {
        // Test success with empty body
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204)); // No Content

        StepVerifier.create(tppConnectorAuth.testConnection("/token", MediaType.APPLICATION_JSON_VALUE, new LinkedMultiValueMap<>()))
                .assertNext(response -> {
                    Assertions.assertEquals("SUCCESS", response.getStatus());
                    Assertions.assertEquals(204, response.getHttpStatus());
                    Assertions.assertNull(response.getResponseBody());
                })
                .verifyComplete();
    }
}