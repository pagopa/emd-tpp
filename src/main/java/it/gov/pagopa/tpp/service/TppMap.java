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

    public Tpp getFromMap(String tppId) {
        return tppCache.get(tppId);
    }

    public Void addToMap(String tppId, Tpp tpp) {
        tppCache.put(tppId, tpp);
        return null;
    }

    public Void removeFromMap(String tppId) {
        tppCache.remove(tppId);
        return null;
    }
}