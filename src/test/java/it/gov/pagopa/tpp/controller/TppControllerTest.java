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

import java.util.ArrayList;
import java.util.List;

@WebFluxTest(TppControllerImpl.class)
class TppControllerTest {

    @MockBean
    private TppServiceImpl tppService;

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void upsert_Ok() {
        TppDTO mockTppDTO = TppDTOFaker.mockInstance(true);

        Mockito.when(tppService.upsert(mockTppDTO)).thenReturn(Mono.just(mockTppDTO));

        webClient.post()
                .uri("/emd/tpp")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mockTppDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TppDTO.class)
                .consumeWith(response -> {
                    TppDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(mockTppDTO, resultResponse);
                });
    }

    @Test
    void stateUpdate_Ok()  {
        TppDTO mockTppDTO = TppDTOFaker.mockInstance(true);

        Mockito.when(tppService.updateState(mockTppDTO.getTppId(), mockTppDTO.getState()))
                .thenReturn(Mono.just(mockTppDTO));

        webClient.put()
                .uri("/emd/tpp")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mockTppDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TppDTO.class)
                .consumeWith(response -> {
                    TppDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(resultResponse,mockTppDTO);
                });
    }

    @Test
    void get_Ok()  {
        TppDTO mockTppDTO = TppDTOFaker.mockInstance(true);

        Mockito.when(tppService.get(mockTppDTO.getTppId()))
                .thenReturn(Mono.just(mockTppDTO));

        webClient.get()
                .uri("/emd/tpp/{tppId}",mockTppDTO.getTppId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(TppDTO.class)
                .consumeWith(response -> {
                    TppDTO resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(resultResponse,mockTppDTO);
                });
    }

    @Test
    void getEnabled_Ok() {
        TppIdList tppIdList = new TppIdList();
        ArrayList<String> tppIds = new ArrayList<>();
        tppIds.add("1");
        tppIds.add("2");
        tppIds.add("3");
        tppIdList.setIds(tppIds); // Using ArrayList


        TppDTO tpp1 = new TppDTO();
        TppDTO tpp2 = new TppDTO();


        ArrayList<TppDTO> tppDTOList = new ArrayList<>();
        tppDTOList.add(tpp1);
        tppDTOList.add(tpp2);

        Mockito.when(tppService.getEnabledList(tppIdList.getIds())).thenReturn(Mono.just(tppDTOList));

        webClient.post()
                .uri("/emd/tpp/list")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tppIdList) // Body of the request
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TppDTO.class)
                .consumeWith(response -> {
                    List<TppDTO> resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(tppDTOList.size(), resultResponse.size());
                    Assertions.assertTrue(resultResponse.containsAll(tppDTOList));
                });
    }
}