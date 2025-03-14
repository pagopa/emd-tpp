package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.model.Tpp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TppMap {
    private final Map<String, Tpp> tppCache = new ConcurrentHashMap<>();

    public Mono<Tpp> getFromMap(String tppId) {
        return Mono.justOrEmpty(tppCache.get(tppId));
    }

    public Mono<Void> addToCache(String tppId, Tpp tpp) {
        tppCache.put(tppId, tpp);
        return Mono.empty();
    }

    public Mono<Void> removeFromCache(String tppId) {
        tppCache.remove(tppId);
        return Mono.empty();
    }
}