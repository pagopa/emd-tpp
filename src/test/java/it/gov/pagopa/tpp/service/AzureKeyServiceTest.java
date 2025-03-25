package it.gov.pagopa.tpp.service;

import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.service.keyvault.AzureKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
@SpringBootTest
@TestPropertySource(properties = "crypto.azure.key-vault.url=https://mock-vault-url")
class AzureKeyServiceTest {

    @Mock
    private KeyAsyncClient mockKeyClient;

    @Mock
    private KeyVaultKey mockKeyVaultKey;
    @Mock
    private CryptographyAsyncClient mockCryptographyClient;

    @Autowired
    private AzureKeyService azureKeyService;

    @BeforeEach
    void setUp() {
        azureKeyService.setKeyClient(mockKeyClient);
    }

    @Test
    void testGetKey() {
        when(mockKeyClient.getKey("tppId")).thenReturn(Mono.just(mock(KeyVaultKey.class)));

        KeyVaultKey key = azureKeyService.getKey("tppId").block();
        assertNotNull(key);
    }

    @Test
    void testCreateRsaKey() {
        when(mockKeyClient.createRsaKey(any(CreateRsaKeyOptions.class))).thenReturn(Mono.just(mock(KeyVaultKey.class)));

        KeyVaultKey key = azureKeyService.createRsaKey("tppId").block();
        assertNotNull(key);
    }

    @Test
    void testEncrypt() {
        byte[] plainText = "test message".getBytes();
        EncryptionAlgorithm algorithm = EncryptionAlgorithm.RSA_OAEP;

        byte[] encryptedBytes = "encrypted message".getBytes();
        when(mockCryptographyClient.encrypt(algorithm, plainText))
                .thenReturn(Mono.just(new com.azure.security.keyvault.keys.cryptography.models.EncryptResult(encryptedBytes, algorithm, "key")));

        String encrypted = azureKeyService.encrypt(plainText, algorithm, mockCryptographyClient).block();
        assertNotNull(encrypted);
        assertEquals(Base64.getEncoder().encodeToString(encryptedBytes), encrypted);
    }

    @Test
    void testDecrypt() {
        String encryptedValue = Base64.getEncoder().encodeToString("encrypted message".getBytes());
        EncryptionAlgorithm algorithm = EncryptionAlgorithm.RSA_OAEP;

        byte[] decryptedBytes = "test message".getBytes();
        when(mockCryptographyClient.decrypt(algorithm, Base64.getDecoder().decode(encryptedValue)))
                .thenReturn(Mono.just(new com.azure.security.keyvault.keys.cryptography.models.DecryptResult(decryptedBytes, algorithm, "key")));

        String decrypted = azureKeyService.decrypt(encryptedValue, algorithm, mockCryptographyClient).block();
        assertNotNull(decrypted);
        assertEquals(new String(decryptedBytes), decrypted);
    }

    @Test
    void testBuildCryptographyClient_WithString() {
        String keyId = "https://test.vault.azure.net/keys/myKey/test";

        CryptographyAsyncClient client1 = azureKeyService.buildCryptographyClient(keyId);
        assertNotNull(client1);
    }

    @Test
    void testBuildCryptographyClient_WithKeyClient() {
        String keyId = "https://test.vault.azure.net/keys/myKey/test";

        when(mockKeyVaultKey.getId()).thenReturn(keyId);

        CryptographyAsyncClient client2 = azureKeyService.buildCryptographyClient(mockKeyVaultKey);
        assertNotNull(client2);
    }

}
