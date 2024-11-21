package it.gov.pagopa.tpp.service.keyvault;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class AzureEncryptService {

    private static final DefaultAzureCredential DEFAULT_AZURE_CREDENTIAL = new DefaultAzureCredentialBuilder().build();

    private final KeyClient keyClient;
    public AzureEncryptService(@Value("${crypto.azure.key-vault.url}") String keyVaultUrl){
        this.keyClient= new KeyClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(DEFAULT_AZURE_CREDENTIAL)
                .buildClient();
    }

    public KeyVaultKey getKey(String tppId){
        return keyClient.getKey(tppId);
    }

    public KeyVaultKey createRsaKey(String tppId){
        return keyClient
                .createRsaKey(new CreateRsaKeyOptions(tppId)
                                  .setExpiresOn(OffsetDateTime.now().plusYears(1))
                                  .setKeySize(2048)
        );
    }


    public static CryptographyClient buildCryptographyClient(KeyVaultKey key){
        return buildCryptographyClient(key.getId());
    }

    public static CryptographyClient buildCryptographyClient(String keyId){
        return new CryptographyClientBuilder()
                .credential(DEFAULT_AZURE_CREDENTIAL)
                .keyIdentifier(keyId)
                .buildClient();
    }

    public static String encrypt(byte[] plainValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyClient cryptoClient) {
        // byte[] -> RSA -> Base64
        return Base64.getEncoder().encodeToString(cryptoClient.encrypt(encryptionAlgorithm, plainValue).getCipherText());
    }

    public static String decrypt(String encryptedValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyClient cryptoClient) {
        // Base64 -> RSA -> byte[]
        return new String(cryptoClient.decrypt(encryptionAlgorithm, Base64.getDecoder().decode(encryptedValue)).getPlainText());
    }
}
