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
        // Clean up previous test data by dropping the entire collection
        StepVerifier.create(
            mongoTemplate.dropCollection(COLLECTION_NAME)
                .onErrorResume(e -> Mono.empty())
        ).verifyComplete();

        // Create first TPP entity with state=TRUE for testing active TPPs
        Tpp testTpp = Tpp.builder()
            .state(true)
                .tppId(TPP_ID)
                .idPsp("idPsp")
                .messageUrl("messageUrl")
                .legalAddress("legalAddress")
                .authenticationUrl("authenticationUrl")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(Boolean.TRUE)  // This TPP is active
                .businessName("businessName")
                .contact(new Contact("name","number","email"))
                .entityId(ENTITY_ID)
                .tokenSection(new TokenSection("",new HashMap<>(), new HashMap<>()))
                .pspDenomination("paymentButton")
                .agentDeepLinks(new HashMap<>())
                .build();

        // Create second TPP entity with state=FALSE for testing inactive TPPs
        Tpp testTpp2 = Tpp.builder()
            .state(true)
                .tppId(TPP_ID_2)
                .idPsp("idPsp")
                .messageUrl("messageUrl")
                .legalAddress("legalAddress")
                .authenticationUrl("authenticationUrl")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(Boolean.FALSE)  // This TPP is inactive
                .businessName("businessName")
                .contact(new Contact("name","number","email"))
                .entityId(ENTITY_ID_2)
                .tokenSection(new TokenSection("",new HashMap<>(), new HashMap<>()))
                .pspDenomination("paymentButton")
                .agentDeepLinks(new HashMap<>())
                .build();

        // Insert both test entities into MongoDB
        StepVerifier.create(
            mongoTemplate.save(testTpp, COLLECTION_NAME)
            .then(mongoTemplate.save(testTpp2, COLLECTION_NAME))
        ).expectNextCount(1).verifyComplete();
    }

    /**
     * Test Case: Successful TPP lookup by tppId
     * 
     * Scenario: Query for an existing TPP using its unique tppId
     * Expected: Should find and return the TPP entity with matching tppId
     * MongoDB Query: db.tpp.findOne({"tppId": "tppId"})
     */
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

    /**
     * Test Case: TPP lookup failure by non-existent tppId
     * 
     * Scenario: Query for a TPP that doesn't exist in the database
     * Expected: Should return empty Mono (no results)
     * Purpose: Verify that the query handles missing records gracefully
     */
    @Test
    void testFindByTppIdNotFound() {
        log.info("=== EXECUTING findByTppId (not found) ===");

        StepVerifier.create(
                repository.findByTppId("wrong_tpp_id")
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    /**
     * Test Case: Successful TPP lookup by entityId
     * 
     * Scenario: Query for an existing TPP using its entityId (business identifier)
     * Expected: Should find and return the TPP entity with matching entityId
     * MongoDB Query: db.tpp.findOne({"entityId": "entityId"})
     */
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

    /**
     * Test Case: TPP lookup failure by non-existent entityId
     * 
     * Scenario: Query for a TPP using an entityId that doesn't exist
     * Expected: Should return empty Mono (no results)
     * Purpose: Verify error handling for invalid entityId lookups
     */
    @Test
    void testFindByEntityIdNotFound() {
        log.info("=== EXECUTING findByEntityId (not found) ===");

        StepVerifier.create(
                repository.findByEntityId("wrong_entity_id")
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    /**
     * Test Case: Find active TPPs by tppId list
     * 
     * Scenario: Query for TPPs that are in a given list of tppIds AND have state=true
     * Expected: Should return only active TPPs (state=true) that match the tppId list
     * MongoDB Query: db.tpp.find({"tppId": {$in: ["tppId"]}, "state": true})
     * 
     * Business Logic: Only active/enabled TPPs should be returned, even if inactive
     * TPPs exist with matching tppIds
     */
    @Test
    void testFindByTppIdAndStateTrue() {
        log.info("=== EXECUTING findByTppIdInAndStateTrue ===");
        List<String> tppIdList = new ArrayList<>();
        tppIdList.add(TPP_ID);  // This TPP has state=true, so should be found
        
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

    /**
     * Test Case: Filter out inactive TPPs from tppId list query
     * 
     * Scenario: Query for TPPs using a tppId that exists but has state=false
     * Expected: Should return empty result because TPP is inactive (state=false)
     * Purpose: Verify that the state filter correctly excludes inactive TPPs
     * 
     * Business Logic: Even if a TPP exists with the requested tppId, it should
     * not be returned if it's marked as inactive (state=false)
     */
    @Test
    void testFindByTppIdAndStateTrueNotFound() {
        log.info("=== EXECUTING findByTppIdInAndStateTrue (not found) ===");
        List<String> tppIdList = new ArrayList<>();
        tppIdList.add(TPP_ID_2);  // This TPP has state=false, so should NOT be found
        
        StepVerifier.create(
                repository.findByTppIdInAndStateTrue(tppIdList)
            )
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

}