package it.gov.pagopa.tpp.repository;

import it.gov.pagopa.tpp.model.Tpp;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link TppRepositoryExtended} based on {@link ReactiveMongoTemplate}.
 * <p>
 * It builds the search {@link Query} dynamically and pushes pagination down to the database
 * through {@code skip}/{@code limit}, avoiding loading the full collection in memory.
 */
@Repository
public class TppRepositoryExtendedImpl implements TppRepositoryExtended {

    private static final String FIELD_ENTITY_ID = "entityId";
    private static final String FIELD_BUSINESS_NAME = "businessName";

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public TppRepositoryExtendedImpl(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flux<Tpp> searchTpps(String entityId, String businessName, int page, int size) {
        Query query = buildCriteriaQuery(entityId, businessName)
                .with(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, FIELD_BUSINESS_NAME)));
        return reactiveMongoTemplate.find(query, Tpp.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Long> countTpps(String entityId, String businessName) {
        return reactiveMongoTemplate.count(buildCriteriaQuery(entityId, businessName), Tpp.class);
    }

    /**
     * Builds the base {@link Query} applying the filtering strategy:
     * exact match on {@code entityId} when present, otherwise a case-insensitive
     * partial match ({@code CONTAINS}) on {@code businessName}.
     *
     * @param entityId     the exact entity identifier to match (nullable)
     * @param businessName the partial, case-insensitive business name to match (nullable)
     * @return the {@link Query} carrying the selected {@link Criteria}
     */
    private Query buildCriteriaQuery(String entityId, String businessName) {
        Query query = new Query();
        if (StringUtils.hasText(entityId)) {
            query.addCriteria(Criteria.where(FIELD_ENTITY_ID).is(entityId));
        } else if (StringUtils.hasText(businessName)) {
            query.addCriteria(Criteria.where(FIELD_BUSINESS_NAME)
                    .regex(Pattern.compile(Pattern.quote(businessName), Pattern.CASE_INSENSITIVE)));
        }
        return query;
    }
}

