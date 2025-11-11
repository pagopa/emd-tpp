package it.gov.pagopa.common.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

/**
 * Configuration class for MongoDB health monitoring components.
 * This configuration class defines beans related to MongoDB health checking
 * in a reactive Spring Boot application. It provides custom health indicators
 * that integrate with Spring Boot Actuator to monitor MongoDB connectivity
 * and status.
 */
@Configuration
public class MongoHealthConfig {
    /**
     * Creates a custom reactive MongoDB health indicator bean.
     * <p>
     * The health indicator performs non-blocking health checks and integrates 
     * with Spring Boot Actuator's health endpoint.
     *
     * @param reactiveMongoTemplate the reactive MongoDB template used for
     *                            executing health check commands against the MongoDB instance
     * @return a {@link CustomReactiveMongoHealthIndicator} instance configured
     *         with the provided reactive MongoDB template
     */
    @Bean
    public CustomReactiveMongoHealthIndicator customMongoHealthIndicator(ReactiveMongoTemplate reactiveMongoTemplate) {
        return new CustomReactiveMongoHealthIndicator(reactiveMongoTemplate);
    }
}

