package it.gov.pagopa.common.configuration;

import org.bson.Document;
import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Custom reactive health indicator for MongoDB.
 */
public class CustomReactiveMongoHealthIndicator extends AbstractReactiveHealthIndicator {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public CustomReactiveMongoHealthIndicator(ReactiveMongoTemplate reactiveMongoTemplate) {
        super("Mongo health check failed");
        Assert.notNull(reactiveMongoTemplate, "ReactiveMongoTemplate must not be null");
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    /**
     * Performs the actual health check by executing the {@code isMaster} command on the
     * MongoDB instance to verify connectivity and retrieve server information. 
     *
     * @param builder the health builder to populate with health information
     * @return a {@link Mono} that emits the Health result when the health check completes. 
     * The health status will be "UP" if the command executes successfully.
     */
    @Override
    protected Mono<Health> doHealthCheck(Health.Builder builder)  {
        Mono<Document> buildInfo = this.reactiveMongoTemplate.executeCommand("{ isMaster: 1 }");
        return buildInfo.map(document -> builderUp(builder, document));
    }

    /**
     * Builds a "UP" health status with additional MongoDB server details.
     *
     * @param builder the health builder to configure
     * @param document the MongoDB command response document containing server information
     * @return a {@link org.springframework.boot.actuator.health.Health} instance
     *         with status "UP" and additional details about the MongoDB server
     */
    private Health builderUp(Health.Builder builder, Document document) {
        return builder.up().withDetail("maxWireVersion", document.getInteger("maxWireVersion")).build();
    }
}
