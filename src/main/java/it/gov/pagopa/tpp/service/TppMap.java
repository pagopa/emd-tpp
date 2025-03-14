package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
@Component
@Slf4j
public class TppMap {
    private final Cache<String, Tpp> tppCache;

    private final TppRepository tppRepository;

    public TppMap(TppRepository tppRepository) {
        this.tppRepository = tppRepository;
        this.tppCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }

    @PostConstruct
    private void populateMap() {
        tppRepository.findAll()
                .filter(Tpp::getState)
                .buffer(100)
                .flatMap(this::addToMap)
                .then(Mono.fromRunnable(() -> log.info("[MAP-INITIALIZER] Population complete")))
                .subscribe();
    }

    private Mono<Void> addToMap(List<Tpp> tpps) {
        tpps.forEach(tpp -> {
            String tppId = tpp.getId();
            if (tppCache.asMap().putIfAbsent(tppId, tpp) != null) {
                log.info("Duplicate TPP ID: {}", tppId);
            } else {
                log.info("Added TPP ID: {}", tppId);
            }
        });
        return Mono.empty();
    }

    public void addToMap(String tppId, Tpp tpp) {
        if (tppCache.asMap().putIfAbsent(tppId, tpp) != null) {
            log.info("Duplicate TPP ID: {}", tppId);
        } else {
            log.info("Added TPP ID: {}", tppId);
        }
    }

    public Tpp getFromMap(String tppId) {
        return tppCache.getIfPresent(tppId);
    }

    public void removeFromMap(String tppId) {
        tppCache.invalidate(tppId);
    }

}