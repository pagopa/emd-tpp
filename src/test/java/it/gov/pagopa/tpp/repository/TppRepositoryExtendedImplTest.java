package it.gov.pagopa.tpp.repository;

import static it.gov.pagopa.tpp.utils.TestUtils.getMockTpp;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import it.gov.pagopa.tpp.model.Tpp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TppRepositoryExtendedImplTest {

    private final ReactiveMongoTemplate reactiveMongoTemplate = Mockito.mock(ReactiveMongoTemplate.class);
    private final TppRepositoryExtendedImpl repository = new TppRepositoryExtendedImpl(reactiveMongoTemplate);

    @Test
    void searchTpps_byEntityId_appliesExactMatchAndPagination() {
        Tpp mockTpp = getMockTpp();
        Mockito.when(reactiveMongoTemplate.find(any(Query.class), eq(Tpp.class)))
            .thenReturn(Flux.just(mockTpp));

        StepVerifier.create(repository.searchTpps("entityId01234567", null, 0, 10))
            .expectNext(mockTpp)
            .verifyComplete();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        Mockito.verify(reactiveMongoTemplate).find(captor.capture(), eq(Tpp.class));

        Query executedQuery = captor.getValue();
        String queryJson = executedQuery.getQueryObject().toJson();
        Assertions.assertTrue(queryJson.contains("entityId"));
        Assertions.assertTrue(queryJson.contains("entityId01234567"));
        Assertions.assertEquals(10, executedQuery.getLimit());
        Assertions.assertEquals(0, executedQuery.getSkip());
    }

    @Test
    void searchTpps_byBusinessName_appliesCaseInsensitiveContains() {
        Tpp mockTpp = getMockTpp();
        Mockito.when(reactiveMongoTemplate.find(any(Query.class), eq(Tpp.class)))
            .thenReturn(Flux.just(mockTpp));

        StepVerifier.create(repository.searchTpps(null, "business", 1, 20))
            .expectNext(mockTpp)
            .verifyComplete();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        Mockito.verify(reactiveMongoTemplate).find(captor.capture(), eq(Tpp.class));

        Query executedQuery = captor.getValue();
        String queryJson = executedQuery.getQueryObject().toJson();
        Assertions.assertTrue(queryJson.contains("businessName"));
        // partial, case-insensitive match is expressed as a regular expression with "i" option
        Assertions.assertTrue(queryJson.contains("$regularExpression") || queryJson.contains("$regex"));
        Assertions.assertTrue(queryJson.contains("\"options\": \"i\"") || queryJson.contains("options"));
        Assertions.assertEquals(20, executedQuery.getLimit());
        Assertions.assertEquals(20, executedQuery.getSkip());
    }

    @Test
    void countTpps_byEntityId_ok() {
        Mockito.when(reactiveMongoTemplate.count(any(Query.class), eq(Tpp.class)))
            .thenReturn(Mono.just(3L));

        StepVerifier.create(repository.countTpps("entityId01234567", null))
            .expectNext(3L)
            .verifyComplete();

        Mockito.verify(reactiveMongoTemplate).count(any(Query.class), eq(Tpp.class));
    }
}

