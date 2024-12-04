package it.gov.pagopa.tpp.repository;


import it.gov.pagopa.tpp.model.Tpp;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TppRepository extends ReactiveMongoRepository<Tpp,String> {

    Mono<Tpp> findByTppIdInAndStateTrue(String tppId);

    Mono<Tpp> findByTppId(String tppId);

    Mono<Tpp> findByEntityId(String entityId);

}
