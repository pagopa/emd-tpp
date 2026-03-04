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
    private static final String TPP_ID_3 = "tppId_3";
    private static final String TPP_ID_4 = "tppId_4";
    private static final String ENTITY_ID = "entityId";
    private static final String ENTITY_ID_2 = "entityId_2";
    private static final String ENTITY_ID_3 = "entityId_3";
    private static final String ENTITY_ID_4 = "entityId_4";
    private static final String COLLECTION_NAME = "tpp";

    private static final String RECIPIENT_ID = "recipient_123";
    private static final String RECIPIENT_ID_2 = "recipient_456";

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
                .agentLinks(new HashMap<>())
                .build();

        // Create second TPP entity with state=FALSE for testing inactive TPPs
        Tpp testTpp2 = Tpp.builder()
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
                .agentLinks(new HashMap<>())
                .build();

        // Create Inactive tpp with whitelist recipient
        Tpp whitelistedTpp = Tpp.builder()
                .tppId(TPP_ID_3)
                .idPsp("idPsp")
                .messageUrl("messageUrl")
                .legalAddress("legalAddress")
                .authenticationUrl("authenticationUrl")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(Boolean.FALSE)  // This TPP is inactive
                .businessName("businessName")
                .contact(new Contact("name","number","email"))
                .entityId(ENTITY_ID_3)
                .tokenSection(new TokenSection("",new HashMap<>(), new HashMap<>()))
                .pspDenomination("paymentButton")
                .agentLinks(new HashMap<>())
                .whitelistRecipient(List.of(RECIPIENT_ID))
                .build();

        // Create Inactive tpp and with different whitelist recipient
        Tpp inactiveTpp = Tpp.builder()
                .tppId(TPP_ID_4)
                .idPsp("idPsp")
                .messageUrl("messageUrl")
                .legalAddress("legalAddress")
                .authenticationUrl("authenticationUrl")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(Boolean.FALSE)  // This TPP is inactive
                .businessName("businessName")
                .contact(new Contact("name","number","email"))
                .entityId(ENTITY_ID_4)
                .tokenSection(new TokenSection("",new HashMap<>(), new HashMap<>()))
                .pspDenomination("paymentButton")
                .agentLinks(new HashMap<>())
                .whitelistRecipient(List.of(RECIPIENT_ID_2)) // Recipient diverso da quello usato nei test
                .build();

        // Insert both test entities into MongoDB
        StepVerifier.create(
            mongoTemplate.save(testTpp, COLLECTION_NAME)
            .then(mongoTemplate.save(testTpp2, COLLECTION_NAME))
            .then(mongoTemplate.save(whitelistedTpp, COLLECTION_NAME))
            .then(mongoTemplate.save(inactiveTpp, COLLECTION_NAME))
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

    /**
     * Test Case: Retrieve inactive TPP via whitelist
     *
     * Scenario: Query for a TPP with state=false, but whose recipientId is present in the whitelistRecipient field
     * Expected: Should find and return the TPP entity despite being globally inactive
     * MongoDB Query: db.tpp.find({"tppId": {$in: ["tppId_3"]}, $or: [{"state": true}, {"whitelistRecipient": "recipientId"}]})
     *
     * Business Logic: A globally disabled TPP must still be allowed to operate if a specific 
     * authorization (whitelist) exists for the requested recipient.
     */
    @Test
    void testFindEnabledForRecipient_WhitelistedTpp() {
        log.info("=== EXECUTING findEnabledForRecipient (Inactive TPP with recipient) ===");
        
        List<String> ids = List.of(TPP_ID_3);
        
        StepVerifier.create(repository.findEnabledForRecipient(ids, RECIPIENT_ID))
             .assertNext(tpp -> {
                assert tpp.getTppId().equals(TPP_ID_3);
                assert tpp.getState().equals(false);
                assert tpp.getWhitelistRecipient().contains(RECIPIENT_ID);
            })
            .verifyComplete();
    }

    /**
     * Test Case: Exclude inactive TPP with unauthorized recipient
     *
     * Scenario: Query for a TPP with state=false and a recipientId that does NOT match the whitelist
     * Expected: Should return an empty result (Flux.empty)
     * Purpose: Verify that the $or condition correctly excludes inactive TPPs without a valid whitelist
     *
     * Business Logic: If a TPP is disabled and the recipient is not among those authorized, 
     * access must be denied.
     */
    @Test
    void testFindEnabledForRecipient_InactiveAndWrongRecipient() {
        log.info("=== EXECUTING findEnabledForRecipient (Inactive & Wrong Recipient) ===");
        
        List<String> ids = List.of(TPP_ID_4);
        
        StepVerifier.create(repository.findEnabledForRecipient(ids, RECIPIENT_ID))
            .verifyComplete(); // No result expected since TPP is inactive and recipient is not whitelisted
    }

    /**
     * Test Case: Retrieve multiple TPPs with mixed enablement criteria
     *
     * Scenario: Query for a list of IDs containing both an active TPP (state=true) and a whitelisted inactive TPP
     * Expected: Should return both TPPs (one satisfying the 'state' clause, the other the 'whitelist' clause)
     * MongoDB Query: Verify that both branches of the $or operator produce valid results
     *
     * Business Logic: The query must correctly aggregate TPPs enabled for different reasons 
     * (global state or specific partner authorization).
     */
    @Test
    void testFindEnabledForRecipient_MixedResults() {
        log.info("=== EXECUTING findEnabledForRecipient (Mixed Results) ===");
        
        List<String> ids = List.of(TPP_ID, TPP_ID_3);
        
        StepVerifier.create(repository.findEnabledForRecipient(ids, RECIPIENT_ID))
            .recordWith(ArrayList::new)
            .expectNextCount(2)
            .consumeRecordedWith(results -> {
                assert results.stream().anyMatch(t -> t.getTppId().equals(TPP_ID));
                assert results.stream().anyMatch(t -> t.getTppId().equals(TPP_ID_3));
            })
            .verifyComplete();
    }
    
    /**
     * Test Case: Mandatory filter by tppId list
     *
     * Scenario: Query for a valid recipient or active state, but using a tppId not present in the database
     * Expected: Should return an empty result
     * Purpose: Verify that the $in clause on the tppId field acts as a mandatory primary filter
     *
     * Business Logic: Even if a TPP is active or whitelisted, it must not be returned if its 
     * identifier is not included in the explicit request list.
     */
    @Test
    void testFindEnabledForRecipient_IdNotInList() {
        log.info("=== EXECUTING findEnabledForRecipient (ID not in list) ===");
        
        List<String> ids = List.of("Wrong_ID");
        
        StepVerifier.create(repository.findEnabledForRecipient(ids, RECIPIENT_ID))
            .verifyComplete();
        log.info("=== TEST COMPLETED ===");
    }

    /**
     * Test Case: Find all TPPs with non-empty whitelist recipients <br/>
     *
     * Scenario: Query for TPPs that have whitelistRecipient field with at least one entry <br/>
     * Expected: Should return only TPPs with populated whitelistRecipient, projecting only tppId and whitelistRecipient fields <br/>
     *
     * Business Logic: Only TPPs with configured whitelist recipients should be returned,
     * and only the relevant fields (tppId, whitelistRecipient)
     */
    @Test
    void testFindAllWhitelistOfTPPs() {
        log.info("=== EXECUTING findAllWhitelistOfTPPs ===");

        StepVerifier.create(
                repository.findAllWhitelistOfTPPs()
            )
            .recordWith(ArrayList::new)
            .expectNextCount(2)
            .consumeRecordedWith(results -> {
                assert results.stream().allMatch(tpp ->
                    tpp.getWhitelistRecipient() != null && !tpp.getWhitelistRecipient().isEmpty()
                ) : "All TPPs should have non-empty whitelistRecipient";

                assert results.stream().anyMatch(t -> t.getTppId().equals(TPP_ID_3));
                assert results.stream().anyMatch(t -> t.getTppId().equals(TPP_ID_4));
            })
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }

    /**
     * Test Case: No TPPs with whitelist recipients
     *
     * Scenario: Query when no TPPs have populated whitelistRecipient field
     * Expected: Should return empty Flux
     * Purpose: Verify query handles empty result set correctly
     */
    @Test
    void testFindAllWhitelistOfTPPs_NoResults() {
        log.info("=== EXECUTING findAllWhitelistOfTPPs (no results) ===");

        // Clean up Before test, remove all TPPs to ensure no whitelist recipients are present
        StepVerifier.create(
            mongoTemplate.dropCollection(COLLECTION_NAME)
                .then(mongoTemplate.save(Tpp.builder()
                    .tppId(TPP_ID)
                    .state(Boolean.TRUE)
                    .entityId(ENTITY_ID)
                    .build(), COLLECTION_NAME))
        ).expectNextCount(1).verifyComplete();

        StepVerifier.create(repository.findAllWhitelistOfTPPs())
            .expectNextCount(0)
            .verifyComplete();

        log.info("=== TEST COMPLETED ===");
    }
}