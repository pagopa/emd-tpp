package it.gov.pagopa.tpp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.tpp.dto.NetworkResponseDTO;
import it.gov.pagopa.tpp.dto.RecipientIdOnWhitelistDTO;
import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.dto.TppIdList;
import it.gov.pagopa.tpp.dto.TppUpdateIsPaymentEnabled;
import it.gov.pagopa.tpp.service.TppServiceImpl;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
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
        // Creiamo un'istanza fresca per questo test
        TppDTOWithoutTokenSection tppDto = getMockTppDtoWithoutTokenSection();

        Mockito.when(tppService.updateTppDetails(tppDto))
            .thenReturn(Mono.just(tppDto));

        webClient.put()
            .uri("/emd/tpp/update")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(tppDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(TppDTOWithoutTokenSection.class)
            .consumeWith(response -> {
                TppDTOWithoutTokenSection resultResponse = response.getResponseBody();
                Assertions.assertNotNull(resultResponse);
                Assertions.assertEquals(tppDto, resultResponse);
            });
    }

    @Test
    void deleteTpp_ok() {
        TppDTO serviceResponse = getMockTppDto();
        TppDTOWithoutTokenSection expectedResponse = getMockTppDtoWithoutTokenSection();

        Mockito.when(tppService.deleteTpp("tppId")).thenReturn(Mono.just(serviceResponse));

        webClient.delete()
            .uri("/emd/tpp/test/delete/{tppId}", "tppId" )
            .exchange()
            .expectStatus().isOk()
            .expectBody(TppDTOWithoutTokenSection.class)
            .consumeWith(response -> {
                TppDTOWithoutTokenSection resultResponse = response.getResponseBody();
                Assertions.assertNotNull(resultResponse);
                Assertions.assertEquals(expectedResponse, resultResponse);
            });
    }

    @Test
    void updateTokenSection_Ok() {
        TokenSectionDTO tokenSectionDTO = getMockTokenSectionDto();

        Mockito.when(tppService.updateTokenSection("tppId", tokenSectionDTO))
            .thenReturn(Mono.just(tokenSectionDTO));

        webClient.put()
            .uri("/emd/tpp/update/{tppId}/token", "tppId" )
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(tokenSectionDTO)
            .exchange()
            .expectStatus().isOk()
            .expectBody(TokenSectionDTO.class)
            .consumeWith(response -> {
                TokenSectionDTO resultResponse = response.getResponseBody();
                Assertions.assertNotNull(resultResponse);
                Assertions.assertEquals(tokenSectionDTO, resultResponse);
            });
    }

    @Test
    void save_Ok() {
        TppDTO tppDto = getMockTppDto();

        Mockito.when(tppService.createNewTpp(eq(tppDto), anyString()))
            .thenReturn(Mono.just(tppDto));

        webClient.post()
            .uri("/emd/tpp/save")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(tppDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(TppDTO.class)
            .consumeWith(response -> {
                TppDTO resultResponse = response.getResponseBody();
                Assertions.assertNotNull(resultResponse);
                Assertions.assertEquals(tppDto, resultResponse);
            });
    }

    @Test
    void stateUpdate_Ok()  {
        TppDTO tppDto = getMockTppDto();

        Mockito.when(tppService.updateState(tppDto.getTppId(), tppDto.getState()))
            .thenReturn(Mono.just(tppDto));

        webClient.put()
            .uri("/emd/tpp")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(tppDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(TppDTO.class)
            .consumeWith(response -> {
                TppDTO resultResponse = response.getResponseBody();
                Assertions.assertNotNull(resultResponse);
                Assertions.assertEquals(tppDto, resultResponse);
            });
    }

    @Test
    void isPaymentEnabled_NoContent()  {
        TppDTO tppDto = getMockTppDto();
        TppUpdateIsPaymentEnabled isPaymentEnabled = getMockIsPaymentEnabled();

        Mockito.when(tppService.updateIsPaymentEnabled(tppDto.getTppId(), isPaymentEnabled.getIsPaymentEnabled()))
            .thenReturn(Mono.just(tppDto));

        webClient.put()
            .uri("/emd/tpp/{tppId}/payment-enabled", tppDto.getTppId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(isPaymentEnabled)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();
    }

    @Test
    void getTppDetails_Ok()  {
        TppDTOWithoutTokenSection tppDtoNoToken = getMockTppDtoWithoutTokenSection();

        Mockito.when(tppService.getTppDetails(tppDtoNoToken.getTppId()))
            .thenReturn(Mono.just(tppDtoNoToken));

        webClient.get()
            .uri("/emd/tpp/{tppId}", tppDtoNoToken.getTppId())
            .exchange()
            .expectStatus().isOk()
            .expectBody(TppDTOWithoutTokenSection.class)
            .consumeWith(response -> {
                TppDTOWithoutTokenSection resultResponse = response.getResponseBody();
                Assertions.assertNotNull(resultResponse);
                Assertions.assertEquals(tppDtoNoToken, resultResponse);
            });
    }

    @Test
    void getTppByEntityId_Ok()  {
        TppDTOWithoutTokenSection tppDtoNoToken = getMockTppDtoWithoutTokenSection();

        Mockito.when(tppService.getTppByEntityId(tppDtoNoToken.getEntityId()))
            .thenReturn(Mono.just(tppDtoNoToken));

        webClient.get()
            .uri("/emd/tpp/entityId/{entityId}", tppDtoNoToken.getEntityId())
            .exchange()
            .expectStatus().isOk()
            .expectBody(TppDTOWithoutTokenSection.class)
            .consumeWith(response -> {
                TppDTOWithoutTokenSection resultResponse = response.getResponseBody();
                Assertions.assertNotNull(resultResponse);
                Assertions.assertEquals(tppDtoNoToken, resultResponse);
            });
    }

    @Test
    void getTokenSection_Ok()  {
        TokenSectionDTO tokenSectionDTO = getMockTokenSectionDto();

        Mockito.when(tppService.getTokenSection("tppId"))
            .thenReturn(Mono.just(tokenSectionDTO));

        webClient.get()
            .uri("/emd/tpp/{tppId}/token", "tppId")
            .exchange()
            .expectStatus().isOk()
            .expectBody(TokenSectionDTO.class)
            .consumeWith(response -> {
                TokenSectionDTO resultResponse = response.getResponseBody();
                Assertions.assertNotNull(resultResponse);
                Assertions.assertEquals(tokenSectionDTO, resultResponse);
            });
    }

    @Test
    void filterEnabled_Ok() {
        TppIdList idList = getMockTppIdList();
        List<TppDTO> dtoList = getMockTppDtoList();

        Mockito.when(tppService.filterEnabledList(idList.getIds(), idList.getRecipientId())).thenReturn(Mono.just(dtoList));

        webClient.post()
            .uri("/emd/tpp/list")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(idList)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(TppDTO.class)
            .consumeWith(response -> {
                List<TppDTO> resultResponse = response.getResponseBody();
                Assertions.assertNotNull(resultResponse);
                Assertions.assertEquals(dtoList.size(), resultResponse.size());
                Assertions.assertTrue(resultResponse.containsAll(dtoList));
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
                // Aggiunto controllo sul contenuto per completezza
                Assertions.assertEquals(networkResponseDTO.getCode(), resultResponse.getCode());
            });
    }

  @Test
  void getAllWhitelistRecipientId_Ok() {
    Map<String, List<String>> whitelistMap = Map.of(
        "TPP001", List.of("REC001", "REC002"),
        "TPP002", List.of("REC003")
    );

    Mockito.when(tppService.getAllWhitelistRecipientId())
        .thenReturn(Mono.just(whitelistMap));

    webClient.get()
        .uri("/emd/tpp/whitelist")
        .exchange()
        .expectStatus().isOk()
        .expectBody(new ParameterizedTypeReference<Map<String, List<String>>>() {})
        .consumeWith(response -> {
          Map<String, List<String>> resultResponse = response.getResponseBody();
          Assertions.assertNotNull(resultResponse);
          Assertions.assertEquals(whitelistMap.size(), resultResponse.size());
          Assertions.assertEquals(whitelistMap, resultResponse);
        });
  }

  @Test
  void getTppWhitelistRecipientId_Ok() {
    List<String> recipientIds = List.of("REC001", "REC002", "REC003");

    Mockito.when(tppService.getTppWhitelistRecipientId("tppId"))
        .thenReturn(Mono.just(recipientIds));

    webClient.get()
        .uri("/emd/tpp/{tppId}/whitelist", "tppId")
        .exchange()
        .expectStatus().isOk()
        .expectBody(new ParameterizedTypeReference<List<String>>() {})
        .consumeWith(response -> {
          List<String> resultResponse = response.getResponseBody();
          Assertions.assertNotNull(resultResponse);
          Assertions.assertEquals(recipientIds.size(), resultResponse.size());
          Assertions.assertTrue(resultResponse.containsAll(recipientIds));
        });
  }

  @Test
  void insertRecipientIdOnWhitelist_Created() {
    RecipientIdOnWhitelistDTO recipientDto = new RecipientIdOnWhitelistDTO();
    recipientDto.setRecipientId("REC001");

    Mockito.when(tppService.insertRecipientIdOnWhitelist("tppId", "REC001"))
        .thenReturn(Mono.empty());

    webClient.post()
        .uri("/emd/tpp/{tppId}/whitelist", "tppId")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(recipientDto)
        .exchange()
        .expectStatus().isCreated()
        .expectBody().isEmpty();
  }

  @Test
  void removeRecipientIdOnWhitelist_NoContent() {
    Mockito.when(tppService.removeRecipientIdOnWhitelist("tppId", "recipientId"))
        .thenReturn(Mono.empty());

    webClient.delete()
        .uri("/emd/tpp/{tppId}/whitelist/{recipientId}", "tppId", "recipientId")
        .exchange()
        .expectStatus().isNoContent()
        .expectBody().isEmpty();
  }

  @Test
  void updateRecipientIdOnWhitelist_NoContent() {
    List<String> recipientIds = List.of("REC001", "REC002", "REC003");

    Mockito.when(tppService.updateRecipientIdOnWhitelist("tppId", recipientIds))
        .thenReturn(Mono.empty());

    webClient.put()
        .uri("/emd/tpp/{tppId}/whitelist", "tppId")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(recipientIds)
        .exchange()
        .expectStatus().isNoContent()
        .expectBody().isEmpty();
  }

}