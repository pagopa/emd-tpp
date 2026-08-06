package it.gov.pagopa.tpp.repository;

import it.gov.pagopa.tpp.model.Tpp;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Custom repository fragment for advanced/paginated TPP queries that cannot be expressed
 * with Spring Data derived query methods, and that require pagination to be pushed down
 * to the database (skip/limit) instead of loading the whole dataset in memory.
 */
public interface TppRepositoryExtended {

    /**
     * Searches TPPs applying an efficient, database-side paginated query.
     * <p>
     * Filtering strategy:
     * <ul>
     *     <li>if {@code entityId} is provided, an exact match ({@code =}) is applied;</li>
     *     <li>otherwise, if {@code businessName} is provided, a case-insensitive partial
     *     match ({@code CONTAINS}) is applied.</li>
     * </ul>
     * Pagination is applied through {@code skip}/{@code limit} so that only the requested
     * page is fetched from the database.
     *
     * @param entityId     the exact entity identifier to match (nullable)
     * @param businessName the partial, case-insensitive business name to match (nullable)
     * @param page         the zero-based page index
     * @param size         the page size (already capped by the service layer)
     * @return a {@link Flux} emitting the {@link Tpp} entities of the requested page
     */
    Flux<Tpp> searchTpps(String entityId, String businessName, int page, int size);

    /**
     * Counts the total number of TPPs matching the given search criteria.
     * Used to build the pagination metadata without loading the matching documents.
     *
     * @param entityId     the exact entity identifier to match (nullable)
     * @param businessName the partial, case-insensitive business name to match (nullable)
     * @return a {@link Mono} emitting the total number of matching documents
     */
    Mono<Long> countTpps(String entityId, String businessName);
}

