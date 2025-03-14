package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TppMap {
    private final Map<String, Tpp> tppCache = new HashMap<>();

    private final TppRepository tppRepository;

    public TppMap(TppRepository tppRepository) {
        this.tppRepository = tppRepository;

    }

    @PostConstruct
    private Mono<Void> populateBloomFilter() {
        return tppRepository.findAll()
                .filter(Tpp::getState)
                .buffer(100)
                .flatMap(this::addToMap)
                .then(Mono.fromRunnable(() -> log.info("[BLOOM-FILTER-INITIALIZER] Population complete")));
    }

    private Mono<Void> addToMap(List<Tpp> tpps) {
        tpps.forEach(tpp -> {
            String tppId = tpp.getId();
            if (tppCache.putIfAbsent(tppId, tpp) != null) {
                log.info("Duplicate TPP ID: {}", tppId);
            } else {
                log.info("Added TPP ID: {}", tppId);
            }
        });
        return Mono.empty();
    }
    public void addToMap(String tppId, Tpp tpp) {
        if (tppCache.putIfAbsent(tppId, tpp) != null) {
            log.info("Duplicate TPP ID: {}", tppId);
        }
        else {
            log.info("Added TPP ID: {}", tppId);
        }
    }

    public Tpp getFromMap(String tppId) {
        return tppCache.get(tppId);
    }

    public Void removeFromMap(String tppId) {
        tppCache.remove(tppId);
        return null;
    }
}