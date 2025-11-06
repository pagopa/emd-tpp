package it.gov.pagopa.tpp.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.TestPropertySource;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;
import it.gov.pagopa.tpp.model.TokenSection;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test verifying MongoDB aggregation queries.
 *
 * <p>Uses MongoDB driver debug logging to inspect generated pipelines.
 * Check console output for actual query structure.</p>
 */
@TestPropertySource(properties = {
    "logging.level.org.springframework.data.mongodb.core.ReactiveMongoTemplate=DEBUG",
})
public class TppRepositoryQueryVerificationIT extends BaseIT{
    private static final Logger log = LoggerFactory.getLogger(TppRepositoryQueryVerificationIT.class);

    private static final String TPP_ID = "tppId";
    private static final String TPP_ID_2 = "tppId_2";
    private static final String ENTITY_ID = "entityId";
    private static final String ENTITY_ID_2 = "entityId_2";
    private static final String COLLECTION_NAME = "tpp";

    @Autowired
    ReactiveMongoTemplate mongoTemplate;

    @Autowired
    TppRepository repository;

    @BeforeEach
    void setup() {
        // Drop collection
        StepVerifier.create(
            mongoTemplate.dropCollection(COLLECTION_NAME)
                .onErrorResume(e -> Mono.empty())
        ).verifyComplete();

        // Insert test data 
        Tpp testTpp = Tpp.builder()
            .state(true)
                .tppId(TPP_ID)
                .idPsp("idPsp")
                .messageUrl("messageUrl")
                .legalAddress("legalAddress")
                .authenticationUrl("authenticationUrl")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(Boolean.TRUE)
                .businessName("businessName")
                .contact(new Contact("name","number","email"))
                .entityId(ENTITY_ID)
                .tokenSection(new TokenSection("",new HashMap<>(), new HashMap<>()))
                .paymentButton("paymentButton")
                .agentDeepLinks(new HashMap<>())
                .build();

        Tpp testTpp2 = Tpp.builder()
            .state(true)
                .tppId(TPP_ID_2)
                .idPsp("idPsp")
                .messageUrl("messageUrl")
                .legalAddress("legalAddress")
                .authenticationUrl("authenticationUrl")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(Boolean.FALSE)
                .businessName("businessName")
                .contact(new Contact("name","number","email"))
                .entityId(ENTITY_ID_2)
                .tokenSection(new TokenSection("",new HashMap<>(), new HashMap<>()))
                .paymentButton("paymentButton")
                .agentDeepLinks(new HashMap<>())
                .build();

        StepVerifier.create(
            mongoTemplate.save(testTpp, COLLECTION_NAME)
            .then(mongoTemplate.save(testTpp2, COLLECTION_NAME))
        ).expectNextCount(1).verifyComplete();
    }

    //tppId
    @Test
    void testFindByTppId() {
        log.info("=== EXECUTING findByTppId ===");

        StepVerifier.create(
                repository.findByTppId(TPP_ID)
            )
            .assertNext(tpp -> {
                log.info("Found tpp: {}", tpp);
                assert tpp.getTppId().equals(TPP_ID);
            })
            .verifyComplete();

        log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
    }

    @Test
    void testFindByTppIdNotFound() {
        log.info("=== EXECUTING findByTppId (not found) ===");

        StepVerifier.create(
                repository.findByTppId("wrong_tpp_id")
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    // entityId
    @Test
    void testFindByEntityId() {
        log.info("=== EXECUTING findByEntityId ===");

        StepVerifier.create(
                repository.findByEntityId(ENTITY_ID)
            )
            .assertNext(tpp -> {
                log.info("Found tpp: {}", tpp);
                assert tpp.getEntityId().equals(ENTITY_ID);
            })
            .verifyComplete();

        log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
    }

    @Test
    void testFindByEntityIdNotFound() {
        log.info("=== EXECUTING findByEntityId (not found) ===");

        StepVerifier.create(
                repository.findByEntityId("wrong_entity_id")
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    // ttpId and state
    @Test
    void testFindByTppIdAndStateTrue() {
        log.info("=== EXECUTING findByTppIdInAndStateTrue ===");
        List<String> tppIdList = new ArrayList<>();
        tppIdList.add(TPP_ID);
        StepVerifier.create(
                repository.findByTppIdInAndStateTrue(tppIdList)
            )
            .assertNext(tpp -> {
                log.info("Found tpp: {}", tpp);
                assert tpp.getTppId().equals(TPP_ID);
            })
            .verifyComplete();

        log.info("=== TEST COMPLETED - CHECK LOGS ABOVE FOR QUERY DETAILS ===");
    }

    @Test
    void testFindByTppIdAndStateTrueNotFound() {
        log.info("=== EXECUTING findByTppIdInAndStateTrue (not found) ===");
        List<String> tppIdList = new ArrayList<>();
        tppIdList.add(TPP_ID_2);
        StepVerifier.create(
                repository.findByTppIdInAndStateTrue(tppIdList)
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

}
