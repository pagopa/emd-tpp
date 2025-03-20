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

@Service
@Slf4j
public class TokenSectionCryptService {

    private final AzureKeyService azureKeyService;

    public TokenSectionCryptService(AzureKeyService azureKeyService) {
        this.azureKeyService = azureKeyService;
    }

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

    public Mono<Boolean> keyDecrypt(TokenSection tokenSection, String tppId) {
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
