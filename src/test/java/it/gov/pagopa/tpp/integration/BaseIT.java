package it.gov.pagopa.tpp.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Base astratta per test di integrazione.
 * Utilizza:
 * - @Testcontainers e @Container per la gestione automatica dei container (avvio/stop).
 * - @DynamicPropertySource per configurare Spring
 */
@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
abstract class BaseIT {

    private static final Logger log = LoggerFactory.getLogger(BaseIT.class);

    @Container
    protected static final MongoDBContainer mongo = new MongoDBContainer("mongo:8.0.15-noble");

    @Autowired
    protected WebTestClient webTestClient;

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        log.info("Configuring Spring properties using Mongo replica set {}",
                mongo.getReplicaSetUrl());

        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }
}