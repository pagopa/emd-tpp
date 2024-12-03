package it.gov.pagopa.tpp.service.keyvault;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.model.TokenSection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class AzureEncryptService {

    private static final DefaultAzureCredential DEFAULT_AZURE_CREDENTIAL = new DefaultAzureCredentialBuilder().build();

    private  KeyClient keyClient;
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


    public String encrypt(byte[] plainValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyClient cryptoClient) {
        return Base64.getEncoder().encodeToString(cryptoClient.encrypt(encryptionAlgorithm, plainValue).getCipherText());
    }

    public String decrypt(String encryptedValue, EncryptionAlgorithm encryptionAlgorithm, CryptographyClient cryptoClient) {
        return new String(cryptoClient.decrypt(encryptionAlgorithm, Base64.getDecoder().decode(encryptedValue)).getPlainText());
    }

    public CryptographyClient buildCryptographyClient(KeyVaultKey key) {
        return buildCryptographyClient(key.getId());
    }

    public CryptographyClient buildCryptographyClient(String keyId) {
        return new CryptographyClientBuilder()
                .credential(DEFAULT_AZURE_CREDENTIAL)
                .keyIdentifier(keyId)
                .buildClient();
    }

    public void setKeyClient(KeyClient keyClient) {
       this.keyClient = keyClient;
    }

    public Mono<Boolean> isKeyExpiring(String tppId) {
        KeyVaultKey key = getKey(tppId);
        KeyProperties properties = key.getProperties();
        return Mono.just(properties.getExpiresOn().isBefore(OffsetDateTime.now().plusDays(7)));
    }

    public void keyEncrypt(TokenSection tokenSection, KeyVaultKey keyVaultKey) {
        CryptographyClient cryptographyClient = buildCryptographyClient(keyVaultKey);
        if(tokenSection.getPathAdditionalProperties() != null && !tokenSection.getBodyAdditionalProperties().isEmpty()){
            tokenSection.getPathAdditionalProperties().replaceAll((key, value) -> encrypt(value.getBytes(), EncryptionAlgorithm.RSA_OAEP_256,cryptographyClient));
        }
        if(tokenSection.getBodyAdditionalProperties() != null && !tokenSection.getBodyAdditionalProperties().isEmpty()){
            tokenSection.getBodyAdditionalProperties().replaceAll((key, value) -> encrypt(value.getBytes(), EncryptionAlgorithm.RSA_OAEP_256,cryptographyClient));
        }
    }

    public void keyDecrypt(TokenSection tokenSection,String tppId) {
        KeyVaultKey keyVaultKey = getKey(tppId);
        CryptographyClient cryptographyClient = buildCryptographyClient(keyVaultKey);
        if(tokenSection.getPathAdditionalProperties() != null && !tokenSection.getBodyAdditionalProperties().isEmpty()){
            tokenSection.getPathAdditionalProperties().replaceAll((key, value) -> decrypt(value, EncryptionAlgorithm.RSA_OAEP_256,cryptographyClient));
        }
        if(tokenSection.getBodyAdditionalProperties() != null && !tokenSection.getBodyAdditionalProperties().isEmpty()){
            tokenSection.getBodyAdditionalProperties().replaceAll((key, value) ->decrypt(value, EncryptionAlgorithm.RSA_OAEP_256,cryptographyClient));
        }
    }
}
