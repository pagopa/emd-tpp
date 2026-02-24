package it.gov.pagopa.tpp.repository;


import it.gov.pagopa.tpp.model.Tpp;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Repository interface for managing TPP entities in MongoDB.
 * <p>
 * Collection name: {@code tpp}
 */
@Repository
public interface TppRepository extends ReactiveMongoRepository<Tpp,String> {

    /**
     * Finds multiple TPP records by their IDs, filtering only active ones.
     * 
     * @param tppIds list of TPP identifiers to search for
     * @return {@link Flux} containing all active Tpp entities matching the provided IDs,
     *         or empty Flux if no active TPPs are found for the given IDs
     */
    Flux<Tpp> findByTppIdInAndStateTrue(List<String> tppIds);

    /**
     * Finds TPP records that are either active or have the recipientId in their whitelist.
     *
     * @param tppIds list of TPP identifiers to search for
     * @param recipientId the recipient identifier to check in whitelist
     * @return {@link Flux} containing matching Tpp entities
     */
    @Query("{ 'tppId': { $in: ?0 }, $or: [ { 'state': true }, { 'whitelistRecipient': ?1 } ] }")
    Flux<Tpp> findEnabledOrWhitelisted(List<String> tppIds, String recipientId);

    /**
     * Finds a single TPP record by its unique TPP identifier.
     * 
     * @param tppId the TPP identifier
     * @return {@link Mono} containing the matching Tpp entity or empty if not found
     */
    Mono<Tpp> findByTppId(String tppId);

    /**
     * Finds a single TPP record by its entity identifier.
     * 
     * @param entityId the P.IVA or C.F. identifier of the TPP
     * @return {@link Mono} containing the matching Tpp entity or empty if not found
     */
    Mono<Tpp> findByEntityId(String entityId);

}
