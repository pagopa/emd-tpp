package it.gov.pagopa.common.configuration;

import com.mongodb.lang.NonNull;
import it.gov.pagopa.common.utils.CommonConstants;
import lombok.Setter;
import org.bson.types.Decimal128;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB configuration class that provides custom settings and data converters.
 */
@Configuration
public class MongoConfig {

      
    /**
     * Configuration properties class for MongoDB custom settings.
     */
    @Configuration
    @ConfigurationProperties(prefix = "spring.data.mongodb.config")
    public static class MongoDbCustomProperties {
        @Setter
        ConnectionPoolSettings connectionPool;

        @Setter
        static class ConnectionPoolSettings {
            int maxSize;
            int minSize;
            long maxWaitTimeMS;
            long maxConnectionLifeTimeMS;
            long maxConnectionIdleTimeMS;
            int maxConnecting;
        }

    }

    /**
     * This customizer configures the MongoDB client's connection pool with the
     * settings defined in the {@link MongoDbCustomProperties}. 
     *
     * @param mongoDbCustomProperties the custom MongoDB properties containing connection pool configuration
     * @return a customizer that applies the connection pool settings
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer customizer(MongoDbCustomProperties mongoDbCustomProperties) {
        return builder -> builder.applyToConnectionPoolSettings(
                connectionPool -> {
                    connectionPool.maxSize(mongoDbCustomProperties.connectionPool.maxSize);
                    connectionPool.minSize(mongoDbCustomProperties.connectionPool.minSize);
                    connectionPool.maxWaitTime(mongoDbCustomProperties.connectionPool.maxWaitTimeMS, TimeUnit.MILLISECONDS);
                    connectionPool.maxConnectionLifeTime(mongoDbCustomProperties.connectionPool.maxConnectionLifeTimeMS, TimeUnit.MILLISECONDS);
                    connectionPool.maxConnectionIdleTime(mongoDbCustomProperties.connectionPool.maxConnectionIdleTimeMS, TimeUnit.MILLISECONDS);
                    connectionPool.maxConnecting(mongoDbCustomProperties.connectionPool.maxConnecting);
                });
    }

    /**
     * Creates a Mongo custom type converters for BigDecimal and OffsetDateTime types.
     * <p>
     * Registers custom converters to handle:
     * <ul>
     *   <li>{@link java.math.BigDecimal} -> {@link org.bson.types.Decimal128} conversion</li>
     *   <li>{@link java.time.OffsetDateTime} -> {@link java.util.Date} conversion</li>
     * </ul>
     * These converters ensure proper serialization and deserialization of Java types
     * that require special handling when stored in MongoDB.
     *
     * @return a {@link MongoCustomConversions} instance with registered custom converters
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
                // BigDecimal support
                new BigDecimalDecimal128Converter(),
                new Decimal128BigDecimalConverter(),

                // OffsetDateTime support
                new OffsetDateTimeWriteConverter(),
                new OffsetDateTimeReadConverter()
        ));
    }

    /**
     * Converter for writing BigDecimal values to MongoDB as Decimal128.
     */
    @WritingConverter
    private static class BigDecimalDecimal128Converter implements Converter<BigDecimal, Decimal128> {

        @Override
        public Decimal128 convert(@NonNull BigDecimal source) {
            return new Decimal128(source);
        }
    }

    /**
     * Converter for reading Decimal128 values from MongoDB as BigDecimal.
     */
    @ReadingConverter
    private static class Decimal128BigDecimalConverter implements Converter<Decimal128, BigDecimal> {

        @Override
        public BigDecimal convert(@NonNull Decimal128 source) {
            return source.bigDecimalValue();
        }

    }

    /**
     * Converter for writing OffsetDateTime values to MongoDB as Date.
     */
    @WritingConverter
    public static class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {
        @Override
        public Date convert(OffsetDateTime offsetDateTime) {
            return Date.from(offsetDateTime.toInstant());
        }
    }

    /**
     * Converter for reading Date values from MongoDB as OffsetDateTime.
     */
    @ReadingConverter
    public static class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(Date date) {
            return date.toInstant().atZone(CommonConstants.ZONEID).toOffsetDateTime();
        }
    }
}

