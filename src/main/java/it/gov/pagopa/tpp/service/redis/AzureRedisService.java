package it.gov.pagopa.tpp.service.redis;

import it.gov.pagopa.tpp.model.Tpp;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AzureRedisService {
    private final RMapReactive<String, Tpp> tppCache;


    public AzureRedisService(RedissonReactiveClient redissonClient) {
        this.tppCache = redissonClient.getMap("tppCache");
    }

    public Mono<Tpp> getFromCache(String tppId) {
        return tppCache.get(tppId);
    }

    public Mono<Void> addToCache(String tppId, Tpp tpp) {
        return tppCache.put(tppId, tpp).then();
    }

    public Mono<Void> removeFromCache(String tppId) {
        return tppCache.remove(tppId).then();
    }
}