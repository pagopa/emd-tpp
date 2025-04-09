package it.gov.pagopa.tpp.stub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tpp.stub.dto.BaseMessageDTO;
import it.gov.pagopa.tpp.stub.dto.TokenDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

@WebFluxTest(StubTppControllerImpl.class)
class StubTppControllerTest {
    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void auth_ok() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("clientId","clinetId");
        formData.add("clientSecret","clientSecret");

        webClient.post()
                .uri("/emd/stub/auth")
                .contentType(MediaType.valueOf(APPLICATION_FORM_URLENCODED_VALUE))
                .bodyValue(formData)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenDTO.class)
                .consumeWith(Assertions::assertNotNull);
    }

    @Test
    void message_ok() {
        BaseMessageDTO messageDTO = new BaseMessageDTO();

        webClient.post()
                .uri("/emd/stub/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(messageDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(Assertions::assertNotNull);
    }


}