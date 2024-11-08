package it.gov.pagopa.tpp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppIdList;
import it.gov.pagopa.tpp.faker.TppDTOFaker;
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

@WebFluxTest(TppControllerImpl.class)
class TppControllerTest {

    @MockBean
    private TppServiceImpl tppService;

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final TppDTO MOCK_TPP_DTO = TppDTOFaker.mockInstance(true);

    private static final List<TppDTO> MOCK_TPP_DTO_LIST = List.of(MOCK_TPP_DTO);
    private static final TppIdList MOCK_TPP_ID_LIST = new TppIdList(List.of(MOCK_TPP_DTO.getTppId()));

    @Test
    void upsert_Ok() {

        Mockito.when(tppService.upsert(MOCK_TPP_DTO)).thenReturn(Mono.just(MOCK_TPP_DTO));

        webClient.post()
                .uri("/emd/tpp")
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
    void get_Ok()  {
        Mockito.when(tppService.get(MOCK_TPP_DTO.getTppId()))
                .thenReturn(Mono.just(MOCK_TPP_DTO));

        webClient.get()
                .uri("/emd/tpp/{tppId}",MOCK_TPP_DTO.getTppId())
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
}