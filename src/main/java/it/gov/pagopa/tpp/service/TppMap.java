package it.gov.pagopa.tpp.service;

import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import it.gov.pagopa.tpp.model.TokenSection;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import it.gov.pagopa.tpp.service.keyvault.AzureEncryptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
@Component
@Slf4j
public class TppMap {
    private final Cache<String, Tpp> tppCache;

    private final TppRepository tppRepository;

    private final AzureEncryptService azureEncryptService;

    public TppMap(TppRepository tppRepository, AzureEncryptService azureEncryptService) {
        this.tppRepository = tppRepository;
        this.azureEncryptService = azureEncryptService;
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
        return Flux.fromIterable(tpps)
                .flatMap(tpp -> {
                    String tppId = tpp.getTppId();
                    return keyDecrypt(tpp.getTokenSection(), tpp.getTppId())
                            .flatMap(decryptionResult -> {
                                if (tppCache.asMap().putIfAbsent(tppId, tpp) != null) {
                                    log.info("Duplicate TPP ID: {}", tppId);
                                } else {
                                    log.info("Added TPP ID: {}", tppId);
                                }
                                return Mono.empty();
                            })
                            .onErrorResume(e -> {
                                log.error("Decryption failed for TPP ID: {}", tppId, e);
                                return Mono.empty();
                            });
                })
                .then();
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

    private Mono<TokenSection> keyDecrypt(TokenSection tokenSection, String tppId) {
        return azureEncryptService.getKey(tppId)
                .flatMap(keyVaultKey -> {
                    CryptographyAsyncClient cryptographyClient = azureEncryptService.buildCryptographyClient(keyVaultKey);
                    Map<String, String> pathProps = tokenSection.getPathAdditionalProperties();
                    Map<String, String> bodyProps = tokenSection.getBodyAdditionalProperties();

                    return Flux.concat(
                            pathProps != null ? Flux.fromIterable(pathProps.entrySet())
                                    .flatMap(entry -> azureEncryptService.decrypt(entry.getValue(), EncryptionAlgorithm.RSA_OAEP_256, cryptographyClient)
                                            .map(entry::setValue)) : Flux.empty(),
                            bodyProps != null ? Flux.fromIterable(bodyProps.entrySet())
                                    .flatMap(entry -> azureEncryptService.decrypt(entry.getValue(), EncryptionAlgorithm.RSA_OAEP_256, cryptographyClient)
                                            .map(entry::setValue)) : Flux.empty()
                    ).then(Mono.just(tokenSection));
                });
    }


}