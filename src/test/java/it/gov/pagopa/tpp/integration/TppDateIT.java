package it.gov.pagopa.tpp.integration;

import com.jayway.jsonpath.JsonPath;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.service.TokenSectionCryptService;
import it.gov.pagopa.tpp.service.TppMapService;
import it.gov.pagopa.tpp.service.keyvault.AzureKeyService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TppDateIT extends BaseIT {

    @Autowired private ReactiveMongoTemplate mongoTemplate;
    @Autowired private TppMapService tppMapService;
    @Autowired private RedissonReactiveClient redis;
    @MockitoBean private TokenSectionCryptService cryptService;
    @MockitoBean private AzureKeyService keyService;

    @BeforeEach
    void setUp() {
        when(cryptService.keyDecrypt(any(), anyString())).thenReturn(Mono.just(true));
        when(cryptService.keyEncrypt(any(), any())).thenReturn(Mono.just(true));
        when(keyService.createRsaKey(anyString())).thenReturn(Mono.just(mock(KeyVaultKey.class)));
        mongoTemplate.dropCollection("tpp").block();
        tppMapService.resetCache();
    }

    @Test
    void bsonInstantsSurviveCacheMissHitAndSearchInAllSeasons() {
        List<String> instants = List.of(
                "2026-01-15T12:34:56.123Z", "2026-07-15T12:34:56.456Z",
                "2026-03-29T00:59:59.999Z", "2026-03-29T01:00:00.001Z",
                "2026-10-25T00:30:00.139Z", "2026-10-25T01:30:00.139Z");
        for (int i = 0; i < instants.size(); i++) {
            String id = "date-" + i;
            String entityId = String.format("%011d", i + 1);
            Instant creation = Instant.parse(instants.get(i));
            Instant updated = creation.plusMillis(216);
            insert(id, entityId, creation, updated);
            Document stored = find(id);
            Instant storedCreation = stored.getDate("creationDate").toInstant();
            Instant storedUpdate = stored.getDate("lastUpdateDate").toInstant();
            assertEquals(creation, storedCreation);
            assertEquals(updated, storedUpdate);

            // First request is a MongoDB cache miss; the next one is a Redis hit.
            for (int n = 0; n < 2; n++) {
                String json = body(webTestClient.get().uri("/emd/tpp/{id}", id));
                assertDate(json, "$.creationDate", storedCreation);
                assertDate(json, "$.lastUpdateDate", storedUpdate);
            }
            var cached = tppMapService.getFromMap(id).block();
            assertNotNull(cached);
            assertEquals(storedCreation, cached.getCreationDate());
            assertEquals(storedUpdate, cached.getLastUpdateDate());
            String byEntity = body(webTestClient.get().uri("/emd/tpp/entityId/{id}", entityId));
            assertDate(byEntity, "$.creationDate", storedCreation);
            assertDate(byEntity, "$.lastUpdateDate", storedUpdate);
            String full = body(webTestClient.get().uri("/emd/tpp/entityId/{id}/full", entityId)
                    .header("X-API-Version", "1"));
            assertDate(full, "$.creationDate", storedCreation);
            assertDate(full, "$.lastUpdateDate", storedUpdate);
            String search = body(webTestClient.get().uri(uri -> uri.path("/emd/tpp/search")
                    .queryParam("entityId", entityId)
                    .queryParam("fields", "creationDate,lastUpdateDate").build()));
            assertDate(search, "$.content[0].creationDate", storedCreation);
            assertDate(search, "$.content[0].lastUpdateDate", storedUpdate);
            String list = body(webTestClient.post().uri("/emd/tpp/list").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("ids", List.of(id), "recipientId", "recipient")));
            assertDate(list, "$[0].creationDate", storedCreation);
            assertDate(list, "$[0].lastUpdateDate", storedUpdate);
        }
    }

    @Test
    void nullDatesAndCacheRepopulation() {
        insert("null-dates", "00000000001", null, null);
        insert("dated", "00000000002", Instant.parse("2026-11-23T11:21:45.139Z"),
                Instant.parse("2026-11-23T14:57:00.355Z"));
        mongoTemplate.insert(new Document("tppId", "explicit-null")
                .append("entityId", "00000000003").append("businessName", "Null dates")
                .append("state", true).append("tokenSection", new Document())
                .append("creationDate", null).append("lastUpdateDate", null), "tpp").block();
        assertTrue(find("explicit-null").containsKey("creationDate"));
        assertNull(find("explicit-null").get("creationDate"));
        redis.<String, String>getMap("emd:tpp:cache", StringCodec.INSTANCE)
                .put("dated", "legacy date without offset").block();
        redis.<String, String>getMap("emd:tpp:entityId-index", StringCodec.INSTANCE)
                .put("00000000002", "dated").block();
        tppMapService.resetCache();
        String nullJson = body(webTestClient.get().uri("/emd/tpp/{id}", "null-dates"));
        assertFalse(nullJson.contains("creationDate"));
        assertFalse(nullJson.contains("lastUpdateDate"));
        String explicitNullJson = body(webTestClient.get().uri("/emd/tpp/{id}", "explicit-null"));
        assertFalse(explicitNullJson.contains("creationDate"));
        assertFalse(explicitNullJson.contains("lastUpdateDate"));
        String datedJson = body(webTestClient.get().uri("/emd/tpp/{id}", "dated"));
        Document stored = find("dated");
        assertDate(datedJson, "$.creationDate", stored.getDate("creationDate").toInstant());
        assertDate(datedJson, "$.lastUpdateDate", stored.getDate("lastUpdateDate").toInstant());
        String nullList = body(webTestClient.post().uri("/emd/tpp/list").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("ids", List.of("null-dates"), "recipientId", "recipient")));
        assertNull(JsonPath.read(nullList, "$[0].creationDate"));
        assertNull(JsonPath.read(nullList, "$[0].lastUpdateDate"));
    }

    @Test
    void patchAndDeleteReturnThePersistedInstants() {
        Instant creation = Instant.parse("2026-10-25T01:30:00.139Z");
        insert("update", "00000000001", creation, creation);
        String patched = body(webTestClient.patch().uri("/emd/tpp/{id}", "update")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("businessName", "Updated")));
        Document stored = find("update");
        assertDate(patched, "$.creationDate", stored.getDate("creationDate").toInstant());
        assertDate(patched, "$.lastUpdateDate", stored.getDate("lastUpdateDate").toInstant());
        String deleted = body(webTestClient.delete().uri("/emd/tpp/{id}", "update"));
        assertDate(deleted, "$.creationDate", stored.getDate("creationDate").toInstant());
        assertDate(deleted, "$.lastUpdateDate", stored.getDate("lastUpdateDate").toInstant());
    }

    @Test
    void createAndUpdateReturnTheInstantsActuallyWrittenToMongo() {
        Map<String, Object> input = Map.of(
                "entityId", "00000000001", "businessName", "New TPP", "authenticationType", "OAUTH2",
                "pspDenomination", "PSP", "agentLinks", Map.of(), "isPaymentEnabled", true,
                "tokenSection", Map.of(), "creationDate", "2026-03-17T17:28:45.058",
                "lastUpdateDate", "2026-03-17T17:28:45.058");
        String created = body(webTestClient.post().uri("/emd/tpp/save")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(input));
        String id = JsonPath.read(created, "$.tppId");
        Document stored = find(id);
        assertDate(created, "$.creationDate", stored.getDate("creationDate").toInstant());
        assertDate(created, "$.lastUpdateDate", stored.getDate("lastUpdateDate").toInstant());

        String updated = body(webTestClient.put().uri("/emd/tpp")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("tppId", id, "state", true)));
        stored = find(id);
        assertDate(updated, "$.creationDate", stored.getDate("creationDate").toInstant());
        assertDate(updated, "$.lastUpdateDate", stored.getDate("lastUpdateDate").toInstant());

        Map<String, Object> details = Map.of("tppId", id, "entityId", "00000000001", "businessName", "Updated",
                "authenticationType", "OAUTH2", "pspDenomination", "PSP", "agentLinks", Map.of(),
                "isPaymentEnabled", true, "creationDate", "2026-03-17T17:28:45.058");
        String edited = body(webTestClient.put().uri("/emd/tpp/update")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(details));
        stored = find(id);
        assertDate(edited, "$.creationDate", stored.getDate("creationDate").toInstant());
        assertDate(edited, "$.lastUpdateDate", stored.getDate("lastUpdateDate").toInstant());
    }

    private void insert(String id, String entityId, Instant creation, Instant updated) {
        Document document = new Document("tppId", id).append("entityId", entityId)
                .append("businessName", "Test TPP").append("state", true)
                .append("tokenSection", new Document());
        if (creation != null) document.append("creationDate", Date.from(creation));
        if (updated != null) document.append("lastUpdateDate", Date.from(updated));
        mongoTemplate.insert(document, "tpp").block();
    }

    private Document find(String id) {
        return mongoTemplate.findOne(Query.query(Criteria.where("tppId").is(id)), Document.class, "tpp").block();
    }

    private static String body(WebTestClient.RequestHeadersSpec<?> request) {
        byte[] response = request.exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        assertNotNull(response);
        return new String(response, StandardCharsets.UTF_8);
    }

    private static void assertDate(String json, String path, Instant expected) {
        String value = JsonPath.read(json, path);
        assertTrue(value.endsWith("Z"), value);
        assertEquals(expected, Instant.parse(value));
    }
}
