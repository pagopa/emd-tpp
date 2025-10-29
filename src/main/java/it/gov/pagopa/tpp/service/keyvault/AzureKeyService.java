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

/**
 * Service class for managing cryptographic operations using Azure Key Vault.
 */
@Service
public class AzureKeyService {

    private static final DefaultAzureCredential DEFAULT_AZURE_CREDENTIAL = new DefaultAzureCredentialBuilder().build();

    private KeyAsyncClient keyClient;

    /**
     * Initializes the KeyAsyncClient using the provided Key Vault URL and
     * DefaultAzureCredential for authentication.
     * 
     * @param keyVaultUrl the URL of the Azure Key Vault instance
     */
    public AzureKeyService(@Value("${crypto.azure.key-vault.url}") String keyVaultUrl){
        this.keyClient= new KeyClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(DEFAULT_AZURE_CREDENTIAL)
                .buildAsyncClient();
    }

    /**
     * Retrieves a key from Azure Key Vault by its identifier.
     * 
     * @param tppId the identifier of the key to retrieve
     * @return a {@link Mono} containing the KeyVaultKey if found
     */
    public Mono<KeyVaultKey> getKey(String tppId){
        return keyClient.getKey(tppId);
    }

    /**
     * Creates a new RSA key in Azure Key Vault with the specified identifier.
     * 
     * @param tppId the identifier for the new RSA key
     * @return a {@link Mono} containing the created KeyVaultKey
     */
    public Mono<KeyVaultKey> createRsaKey(String tppId){
        return keyClient
                .createRsaKey(new CreateRsaKeyOptions(tppId)
                                  .setExpiresOn(OffsetDateTime.now().plusYears(1))
                                  .setKeySize(2048)
        );
    }

    /**
     * Encrypts the provided plain text using the specified encryption algorithm.
     * 
     * @param plainValue the byte array to encrypt
     * @param encryptionAlgorithm the encryption algorithm to use
     * @param cryptoClient the cryptography client for performing the operation
     * @return a {@link Mono} containing the Base64-encoded encrypted string
     */
    public Mono<String> encrypt(byte[] plainValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyAsyncClient cryptoClient) {
        return cryptoClient.encrypt(encryptionAlgorithm, plainValue)
                .map(encryptedData -> Base64.getEncoder().encodeToString(encryptedData.getCipherText()));
    }

    /**
     * Decrypts the provided Base64-encoded encrypted value using the specified encryption algorithm.
     *  
     * @param encryptedValue the Base64-encoded encrypted string to decrypt
     * @param encryptionAlgorithm the encryption algorithm to use for decryption
     * @param cryptoClient the cryptography client for performing the operation
     * @return a {@link Mono} containing the decrypted plain text string
     */
    public Mono<String> decrypt(String encryptedValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyAsyncClient cryptoClient) {
        return cryptoClient.decrypt(encryptionAlgorithm, Base64.getDecoder().decode(encryptedValue))
                .map(decryptedData -> new String(decryptedData.getPlainText()));
    }

    /**
     * Creates a CryptographyAsyncClient for the specified KeyVaultKey.
     * 
     * @param key the KeyVaultKey to create a cryptography client for
     * @return a CryptographyAsyncClient configured for the specified key
     */
    public CryptographyAsyncClient buildCryptographyClient(KeyVaultKey key) {
        return buildCryptographyClient(key.getId());
    }

    /**
     * Creates a CryptographyAsyncClient for the specified key identifier.
     * 
     * @param keyId the identifier of the key to create a cryptography client for
     * @return a CryptographyAsyncClient configured for the specified key identifier
     */
    public CryptographyAsyncClient buildCryptographyClient(String keyId) {
        return new CryptographyClientBuilder()
                .credential(DEFAULT_AZURE_CREDENTIAL)
                .keyIdentifier(keyId)
                .buildAsyncClient();
    }

    /**
     * Sets the KeyAsyncClient for this service.
     * 
     * @param keyClient the KeyAsyncClient to set
     */
    public void setKeyClient(KeyAsyncClient keyClient) {
       this.keyClient = keyClient;
    }

}
