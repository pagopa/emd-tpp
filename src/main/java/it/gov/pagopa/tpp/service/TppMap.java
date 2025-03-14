package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.model.Tpp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TppMap {
    private final Map<String, Tpp> tppCache = new HashMap<>();

    public Tpp getFromMap(String tppId) {
        return tppCache.get(tppId);
    }

    public void addToMap(String tppId, Tpp tpp) {
        if (tppCache.putIfAbsent(tppId, tpp) != null) {
            log.info("Duplicate TPP ID: {}", tppId);
        }
    }

    public Void removeFromMap(String tppId) {
        tppCache.remove(tppId);
        return null;
    }
}