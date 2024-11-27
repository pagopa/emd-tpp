package it.gov.pagopa.tpp.service;

import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.service.keyvault.AzureEncryptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = "crypto.azure.key-vault.url=https://mock-vault-url")
class AzureEncryptServiceTest {

    @Mock
    private KeyClient mockKeyClient;

    @Mock
    private KeyVaultKey mockKeyVaultKey;
    @Mock
    private CryptographyClient mockCryptographyClient;

    @Autowired
    private AzureEncryptService azureEncryptService;

    @BeforeEach
    void setUp() {
        azureEncryptService.setKeyClient(mockKeyClient);
    }

    @Test
    void testGetKey() {
        when(mockKeyClient.getKey("tppId")).thenReturn(mock(KeyVaultKey.class));

        KeyVaultKey key = azureEncryptService.getKey("tppId");
        assertNotNull(key);
    }

    @Test
    void testCreateRsaKey() {
        when(mockKeyClient.createRsaKey(any(CreateRsaKeyOptions.class))).thenReturn(mock(KeyVaultKey.class));

        KeyVaultKey key = azureEncryptService.createRsaKey("tppId");
        assertNotNull(key);
    }

    @Test
    void testEncrypt() {
        byte[] plainText = "test message".getBytes();
        EncryptionAlgorithm algorithm = EncryptionAlgorithm.RSA_OAEP;

        byte[] encryptedBytes = "encrypted message".getBytes();
        when(mockCryptographyClient.encrypt(algorithm, plainText))
                .thenReturn(new com.azure.security.keyvault.keys.cryptography.models.EncryptResult(encryptedBytes, algorithm, "key"));

        String encrypted = azureEncryptService.encrypt(plainText, algorithm, mockCryptographyClient);
        assertNotNull(encrypted);
        assertEquals(Base64.getEncoder().encodeToString(encryptedBytes), encrypted);
    }

    @Test
    void testDecrypt() {
        String encryptedValue = Base64.getEncoder().encodeToString("encrypted message".getBytes());
        EncryptionAlgorithm algorithm = EncryptionAlgorithm.RSA_OAEP;

        byte[] decryptedBytes = "test message".getBytes();
        when(mockCryptographyClient.decrypt(algorithm, Base64.getDecoder().decode(encryptedValue)))
                .thenReturn(new com.azure.security.keyvault.keys.cryptography.models.DecryptResult(decryptedBytes, algorithm, "key"));

        String decrypted = azureEncryptService.decrypt(encryptedValue, algorithm, mockCryptographyClient);
        assertNotNull(decrypted);
        assertEquals(new String(decryptedBytes), decrypted);
    }

    @Test
    void testBuildCryptographyClient_WithString() {
        String keyId = "https://test.vault.azure.net/keys/myKey/test";

        CryptographyClient client1 = azureEncryptService.buildCryptographyClient(keyId);
        assertNotNull(client1);
    }

    @Test
    void testBuildCryptographyClient_WithKeyClient() {
        String keyId = "https://test.vault.azure.net/keys/myKey/test";

        when(mockKeyVaultKey.getId()).thenReturn(keyId);

        CryptographyClient client2 = azureEncryptService.buildCryptographyClient(mockKeyVaultKey);
        assertNotNull(client2);
    }
}
