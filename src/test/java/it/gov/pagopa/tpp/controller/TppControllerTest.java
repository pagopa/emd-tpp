package it.gov.pagopa.tpp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tpp.dto.NetworkResponseDTO;
import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.service.TppServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.gov.pagopa.tpp.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.*;

@WebFluxTest(TppControllerImpl.class)
class TppControllerTest {

    @MockBean
    private TppServiceImpl tppService;

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void updateTppDetails_Ok() {

        Mockito.when(tppService.updateTppDetails(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION)).thenReturn(Mono.just(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION));

        webClient.put()
                .uri("/emd/tpp/update")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TppDTOWithoutTokenSection.class)
                .consumeWith(response -> {
                    TppDTOWithoutTokenSection resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION, resultResponse);
                });
    }

    @Test
    void deleteTpp_ok() {

        Mockito.when(tppService.deleteTpp("tppId")).thenReturn(Mono.just(MOCK_TPP_DTO));

        webClient.delete()
                .uri("/emd/tpp/test/delete/{tppId}", "tppId" )
                .exchange()
                .expectStatus().isOk()
                .expectBody(TppDTOWithoutTokenSection.class)
                .consumeWith(response -> {
                    TppDTOWithoutTokenSection resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION, resultResponse);
                });
    }

    @Test
    void updateTokenSection_Ok() {

        Mockito.when(tppService.updateTokenSection("tppId", MOCK_TOKEN_SECTION_DTO)).thenReturn(Mono.just(MOCK_TOKEN_SECTION_DTO));

        webClient.put()
                .uri("/emd/tpp/update/{tppId}/token", "tppId" )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(MOCK_TOKEN_SECTION_DTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenSectionDTO.class)
                .consumeWith(response -> {
                    TokenSectionDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(MOCK_TOKEN_SECTION_DTO, resultResponse);
                });
    }

    @Test
    void save_Ok() {

        Mockito.when(tppService.createNewTpp(eq(MOCK_TPP_DTO), anyString())).thenReturn(Mono.just(MOCK_TPP_DTO));

        webClient.post()
                .uri("/emd/tpp/save")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(MOCK_TPP_DTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TppDTO.class)
                .consumeWith(response -> {
                    TppDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(MOCK_TPP_DTO, resultResponse);
                });
    }

    @Test
    void stateUpdate_Ok()  {
        Mockito.when(tppService.updateState(MOCK_TPP_DTO.getTppId(), MOCK_TPP_DTO.getState()))
                .thenReturn(Mono.just(MOCK_TPP_DTO));

        webClient.put()
                .uri("/emd/tpp")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(MOCK_TPP_DTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TppDTO.class)
                .consumeWith(response -> {
                    TppDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(MOCK_TPP_DTO,resultResponse);
                });
    }

    @Test
    void isPaymentEnabled_NoContent()  {
        Mockito.when(tppService.updateIsPaymentEnabled(MOCK_TPP_DTO.getTppId(), MOCK_IS_PAYMENT_ENABLED.getIsPaymentEnabled()))
                .thenReturn(Mono.just(MOCK_TPP_DTO));

        webClient.put()
                .uri("/emd/tpp/{tppId}/payment-enabled", MOCK_TPP_DTO.getTppId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(MOCK_IS_PAYMENT_ENABLED)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void getTppDetails_Ok()  {
        Mockito.when(tppService.getTppDetails(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION));

        webClient.get()
                .uri("/emd/tpp/{tppId}",MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getTppId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(TppDTOWithoutTokenSection.class)
                .consumeWith(response -> {
                    TppDTOWithoutTokenSection resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION,resultResponse);
                });
    }

    @Test
    void getTppByEntityId_Ok()  {
        Mockito.when(tppService.getTppByEntityId(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getEntityId()))
                .thenReturn(Mono.just(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION));

        webClient.get()
                .uri("/emd/tpp/entityId/{entityId}",MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION.getEntityId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(TppDTOWithoutTokenSection.class)
                .consumeWith(response -> {
                    TppDTOWithoutTokenSection resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION,resultResponse);
                });
    }

    @Test
    void getTokenSection_Ok()  {
        Mockito.when(tppService.getTokenSection("tppId"))
                .thenReturn(Mono.just(MOCK_TOKEN_SECTION_DTO));

        webClient.get()
                .uri("/emd/tpp/{tppId}/token", "tppId")
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenSectionDTO.class)
                .consumeWith(response -> {
                    TokenSectionDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(MOCK_TOKEN_SECTION_DTO,resultResponse);
                });
    }

    @Test
    void getEnabled_Ok() {
        Mockito.when(tppService.getEnabledList(MOCK_TPP_ID_LIST.getIds())).thenReturn(Mono.just(MOCK_TPP_DTO_LIST));

        webClient.post()
                .uri("/emd/tpp/list")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(MOCK_TPP_ID_LIST) // Body of the request
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TppDTO.class)
                .consumeWith(response -> {
                    List<TppDTO> resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(MOCK_TPP_DTO_LIST.size(), resultResponse.size());
                    Assertions.assertTrue(resultResponse.containsAll(MOCK_TPP_DTO_LIST));
                });
    }

    @Test
    void testConnection() {
        NetworkResponseDTO networkResponseDTO = new NetworkResponseDTO();
        networkResponseDTO.setMessage("tppName ha raggiunto i nostri sistemi");
        networkResponseDTO.setCode("PAGOPA_NETWORK_TEST");
        Mockito.when(tppService.testConnection("tppName")).thenReturn(Mono.just(networkResponseDTO));

        webClient.get()
                .uri("/emd/tpp/network/connection/{tppName}","tppName")
                .exchange()
                .expectStatus().isOk()
                .expectBody(NetworkResponseDTO.class)
                .consumeWith(response -> {
                    NetworkResponseDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                });
    }
}