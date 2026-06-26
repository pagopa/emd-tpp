package it.gov.pagopa.tpp.service;

import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.models.DecryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import it.gov.pagopa.tpp.service.keyvault.AzureKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.redisson.api.RLockReactive;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "crypto.azure.key-vault.url=https://mock-vault",
        "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV4",
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class AzureKeyServiceTest {

    @MockitoBean
    private TppRepository repository;

    @MockitoBean
    private TokenSectionCryptService tokenSectionCryptService;

    @MockitoBean
    private RMapReactive<String, Tpp> tppMap;

    @MockitoBean
    private RedissonReactiveClient redissonReactiveClient;

    @MockitoBean
    private KeyAsyncClient keyClient;

    @MockitoBean
    private KeyVaultKey keyVaultKey;

    @MockitoBean
    private CryptographyAsyncClient cryptographyClient;

    @Autowired
    private AzureKeyService azureKeyService;

    @BeforeEach
    void setup() {

        azureKeyService.setKeyClient(keyClient);

        RLockReactive lock = mock(RLockReactive.class);

        when(redissonReactiveClient.getLock(anyString()))
                .thenReturn(lock);

        when(lock.tryLock(anyLong(), anyLong(), any()))
                .thenReturn(Mono.just(true));

        when(lock.forceUnlock())
                .thenReturn(Mono.just(true));

        when(tppMap.isExists())
                .thenReturn(Mono.just(true));

        when(repository.findAll())
                .thenReturn(Flux.empty());
    }

    @Test
    void testGetKey() {

        when(keyClient.getKey("tppId"))
                .thenReturn(Mono.just(keyVaultKey));

        assertNotNull(
                azureKeyService.getKey("tppId").block()
        );
    }

    @Test
    void testCreateRsaKey() {

        when(keyClient.createRsaKey(any(CreateRsaKeyOptions.class)))
                .thenReturn(Mono.just(keyVaultKey));

        assertNotNull(
                azureKeyService.createRsaKey("tppId").block()
        );
    }

    @Test
    void testEncrypt() {

        byte[] plain = "hello".getBytes();
        byte[] encrypted = "encrypted".getBytes();

        when(cryptographyClient.encrypt(
                EncryptionAlgorithm.RSA_OAEP,
                plain))
                .thenReturn(Mono.just(
                        new EncryptResult(
                                encrypted,
                                EncryptionAlgorithm.RSA_OAEP,
                                "kid")));

        String result = azureKeyService.encrypt(
                plain,
                EncryptionAlgorithm.RSA_OAEP,
                cryptographyClient).block();

        assertEquals(
                Base64.getEncoder().encodeToString(encrypted),
                result);
    }

    @Test
    void testDecrypt() {

        byte[] decrypted = "hello".getBytes();

        String encrypted =
                Base64.getEncoder()
                        .encodeToString("encrypted".getBytes());

        when(cryptographyClient.decrypt(
                EncryptionAlgorithm.RSA_OAEP,
                Base64.getDecoder().decode(encrypted)))
                .thenReturn(Mono.just(
                        new DecryptResult(
                                decrypted,
                                EncryptionAlgorithm.RSA_OAEP,
                                "kid")));

        assertEquals(
                "hello",
                azureKeyService.decrypt(
                        encrypted,
                        EncryptionAlgorithm.RSA_OAEP,
                        cryptographyClient).block());
    }

    @Test
    void testBuildCryptographyClientFromString() {

        assertNotNull(
                azureKeyService.buildCryptographyClient(
                        "https://vault/keys/test/version"));
    }

    @Test
    void testBuildCryptographyClientFromKey() {

        when(keyVaultKey.getId())
                .thenReturn("https://vault/keys/test/version");

        assertNotNull(
                azureKeyService.buildCryptographyClient(keyVaultKey));
    }

}