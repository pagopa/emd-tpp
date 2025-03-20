package it.gov.pagopa.tpp.service;

import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
@Component
@Slf4j
public class TppMapService {
    private final Cache<String, Tpp> tppCache;

    private final TppRepository tppRepository;

    private final TokenSectionCryptService tokenSectionCryptService;

    public TppMapService(TppRepository tppRepository, TokenSectionCryptService tokenSectionCryptService) {
        this.tppRepository = tppRepository;
        this.tokenSectionCryptService = tokenSectionCryptService;
        this.tppCache = Caffeine.newBuilder()
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

    @Scheduled(cron = "0 0 4 * * ?")
    public void resetCache() {
        tppCache.invalidateAll();
        populateMap();
        log.info("[CACHE-RESET] Cache reset at 4 AM");
    }
    private Mono<Void> addToMap(List<Tpp> tpps) {
        return Flux.fromIterable(tpps)
                .flatMap(this::addToMap)
                .then();
    }

    public Mono<Boolean> addToMap(Tpp tpp) {
        String tppId = tpp.getTppId();
        return tokenSectionCryptService.keyDecrypt(tpp.getTokenSection(), tpp.getTppId())
                .flatMap(decryptionResult -> {
                    if (tppCache.asMap().putIfAbsent(tppId, tpp) != null) {
                        log.info("Duplicate TPP ID: {}", tppId);
                        return Mono.just(true);
                    } else {
                        log.info("Added TPP ID: {}", tppId);
                        return Mono.just(false);
                    }

                })
                .onErrorResume(e -> {
                    log.error("Decryption failed for TPP ID: {}", tppId, e);
                    return Mono.just(false);
                });
    }

    public Tpp getFromMap(String tppId) {
        return tppCache.getIfPresent(tppId);
    }

    public void removeFromMap(String tppId) {
        tppCache.invalidate(tppId);
    }

}