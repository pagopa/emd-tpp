package it.gov.pagopa.tpp.service.keyvault;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyAsyncClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class AzureEncryptService {

    private static final DefaultAzureCredential DEFAULT_AZURE_CREDENTIAL = new DefaultAzureCredentialBuilder().build();

    private KeyAsyncClient keyClient;
    public AzureEncryptService(@Value("${crypto.azure.key-vault.url}") String keyVaultUrl){
        this.keyClient= new KeyClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(DEFAULT_AZURE_CREDENTIAL)
                .buildAsyncClient();
    }

    public Mono<KeyVaultKey> getKey(String tppId){
        return keyClient.getKey(tppId);
    }

    public Mono<KeyVaultKey> createRsaKey(String tppId){
        return keyClient
                .createRsaKey(new CreateRsaKeyOptions(tppId)
                                  .setExpiresOn(OffsetDateTime.now().plusYears(1))
                                  .setKeySize(2048)
        );
    }


    public Mono<String> encrypt(byte[] plainValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyAsyncClient cryptoClient) {
        return cryptoClient.encrypt(encryptionAlgorithm, plainValue)
                .map(encryptedData -> Base64.getEncoder().encodeToString(encryptedData.getCipherText()));
    }

    public Mono<String> decrypt(String encryptedValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyAsyncClient cryptoClient) {
        return cryptoClient.decrypt(encryptionAlgorithm, Base64.getDecoder().decode(encryptedValue))
                .map(decryptedData -> new String(decryptedData.getPlainText()));
    }

    public CryptographyAsyncClient buildCryptographyClient(KeyVaultKey key) {
        return buildCryptographyClient(key.getId());
    }

    public CryptographyAsyncClient buildCryptographyClient(String keyId) {
        return new CryptographyClientBuilder()
                .credential(DEFAULT_AZURE_CREDENTIAL)
                .keyIdentifier(keyId)
                .buildAsyncClient();
    }

    public void setKeyClient(KeyAsyncClient keyClient) {
       this.keyClient = keyClient;
    }
}
