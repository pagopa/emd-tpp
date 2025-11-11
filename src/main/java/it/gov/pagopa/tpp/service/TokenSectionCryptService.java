package it.gov.pagopa.tpp.service;

import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.model.TokenSection;
import it.gov.pagopa.tpp.service.keyvault.AzureKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Service class for encrypting and decrypting TokenSection properties using Azure Key Vault.
 */
@Service
@Slf4j
public class TokenSectionCryptService {

    private final AzureKeyService azureKeyService;

    /**
     * Constructs a TokenSectionCryptService with the specified AzureKeyService.
     * 
     * @param azureKeyService the Azure Key Vault service for cryptographic operations
     */
    public TokenSectionCryptService(AzureKeyService azureKeyService) {
        this.azureKeyService = azureKeyService;
    }

    /**
     * Encrypts all values in the TokenSection's path and body additional properties
     * using the provided KeyVaultKey and RSA-OAEP-256 encryption algorithm. 
     * The encryption is performed in-place, modifying the original map values.
     * 
     * @param tokenSection the TokenSection containing properties to encrypt
     * @param keyVaultKey the Azure Key Vault key to use for encryption
     * @return a Mono<Boolean> that emits true when all encryption operations complete successfully
     */
    public Mono<Boolean> keyEncrypt(TokenSection tokenSection, KeyVaultKey keyVaultKey) {
        CryptographyAsyncClient cryptographyClient = azureKeyService.buildCryptographyClient(keyVaultKey);
        Map<String, String> pathProps = tokenSection.getPathAdditionalProperties();
        Map<String, String> bodyProps = tokenSection.getBodyAdditionalProperties();

        return Flux.concat(
                pathProps != null ? Flux.fromIterable(pathProps.entrySet())
                        .flatMap(entry -> azureKeyService.encrypt(entry.getValue().getBytes(), EncryptionAlgorithm.RSA_OAEP_256, cryptographyClient)
                                .map(entry::setValue)) : Flux.empty(),
                bodyProps != null ? Flux.fromIterable(bodyProps.entrySet())
                        .flatMap(entry ->azureKeyService.encrypt(entry.getValue().getBytes(), EncryptionAlgorithm.RSA_OAEP_256, cryptographyClient)
                                .map(entry::setValue)) : Flux.empty()
        ).then(Mono.just(true));
    }

    /**
     * Decrypts all values in the TokenSection's path and body additional properties.
     * <p>
     * This method retrieves the encryption key from Azure Key Vault using the provided
     * TPP ID, then decrypts all string values in both pathAdditionalProperties and
     * bodyAdditionalProperties maps using RSA-OAEP-256 decryption algorithm.
     * The decryption is performed in-place, modifying the original map values.
     * 
     * @param tokenSection the TokenSection containing encrypted properties to decrypt
     * @param tppId the TPP identifier used to retrieve the decryption key from Azure Key Vault
     * @return a Mono<Boolean> that emits true when all decryption operations complete successfully
     */
    public Mono<Boolean> keyDecrypt(TokenSection tokenSection, String tppId) {
        //Recupera KeyVaultKey da azure
        return azureKeyService.getKey(tppId)
                .flatMap(keyVaultKey -> {
                    CryptographyAsyncClient cryptographyClient = azureKeyService.buildCryptographyClient(keyVaultKey);
                    Map<String, String> pathProps = tokenSection.getPathAdditionalProperties();
                    Map<String, String> bodyProps = tokenSection.getBodyAdditionalProperties();

                    return Flux.concat(
                            pathProps != null ? Flux.fromIterable(pathProps.entrySet())
                                    .flatMap(entry -> azureKeyService.decrypt(entry.getValue(), EncryptionAlgorithm.RSA_OAEP_256, cryptographyClient)
                                            .map(entry::setValue)) : Flux.empty(),
                            bodyProps != null ? Flux.fromIterable(bodyProps.entrySet())
                                    .flatMap(entry -> azureKeyService.decrypt(entry.getValue(), EncryptionAlgorithm.RSA_OAEP_256, cryptographyClient)
                                            .map(entry::setValue)) : Flux.empty()
                    ).then(Mono.just(true));
                });
    }
}
