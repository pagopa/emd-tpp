package it.gov.pagopa.common.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.tpp.model.Tpp;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis configuration for reactive TPP caching via Redisson.
 * <p>
 * Provides an {@link RMapReactive} bean backed by a Redis Hash, using Jackson JSON
 * serialization (with Java Time support) for {@link Tpp} objects.
 * Uses the same {@code redisson-spring-boot-starter} as emd-citizen for ecosystem consistency.
 * </p>
 */
@Configuration
public class RedisConfig {

    public static final String TPP_CACHE_MAP_KEY = "emd:tpp:cache";

    @Bean
    public RMapReactive<String, Tpp> tppMapReactive(RedissonReactiveClient redissonReactiveClient) {
        ObjectMapper redisObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return redissonReactiveClient.getMap(TPP_CACHE_MAP_KEY, new JsonJacksonCodec(redisObjectMapper));
    }
}
