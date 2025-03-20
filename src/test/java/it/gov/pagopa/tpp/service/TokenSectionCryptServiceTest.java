package it.gov.pagopa.tpp.service;

import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.service.keyvault.AzureKeyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.tpp.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        AzureKeyService.class,
        TokenSectionCryptService.class
})
class TokenSectionCryptServiceTest {

    @MockBean
    private AzureKeyService azureKeyService;

    @Mock
    private KeyVaultKey mockKeyVaultKey;
    @Mock
    private CryptographyAsyncClient mockCryptographyClient;

    @Autowired
    private TokenSectionCryptService tppTokenSectionCryptService;

    @Test
    void testKeyEncrypt(){

        when(azureKeyService.buildCryptographyClient(mockKeyVaultKey)).thenReturn(mockCryptographyClient);
        when(azureKeyService.encrypt(any(byte[].class), eq(EncryptionAlgorithm.RSA_OAEP_256), eq(mockCryptographyClient))).thenReturn(Mono.just("test"));

        Boolean result = tppTokenSectionCryptService.keyEncrypt(MOCK_TOKEN_SECTION,mockKeyVaultKey).block();

        assertTrue(result);
    }

    @Test
    void testKeyDecrypt(){

        when(azureKeyService.getKey("tppId")).thenReturn(Mono.just(mockKeyVaultKey));
        when(azureKeyService.buildCryptographyClient(mockKeyVaultKey)).thenReturn(mockCryptographyClient);
        when(azureKeyService.decrypt(anyString(), eq(EncryptionAlgorithm.RSA_OAEP_256), eq(mockCryptographyClient))).thenReturn(Mono.just("test"));

        Boolean result = tppTokenSectionCryptService.keyDecrypt(MOCK_TOKEN_SECTION, "tppId").block();

        assertTrue(result);

    }


}
