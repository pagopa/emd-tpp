package it.gov.pagopa.tpp.repository;


import it.gov.pagopa.tpp.model.Tpp;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface TppRepository extends ReactiveMongoRepository<Tpp,String> {

    Flux<Tpp> findByTppIdInAndStateTrue(List<String> tppIds);

    Mono<Tpp> findByTppId(String tppId);

    Mono<Tpp> findByEntityId(String entityId);

    Mono<Tpp> deleteByTppId(String tppId);
}
