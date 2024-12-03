package it.gov.pagopa.tpp.service.keyvault;

import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class KeyRenewalScheduler {

    private final TppRepository tppRepository;

    private final AzureEncryptService azureEncryptService;

    public KeyRenewalScheduler(TppRepository tppRepository, AzureEncryptService azureEncryptService) {
        this.tppRepository = tppRepository;
        this.azureEncryptService = azureEncryptService;
    }

    @Scheduled(fixedRate = 60000) // Esegue ogni minuto
    public void renewExpiringKeys() {
        tppRepository.findAll()
                .doOnNext(this::checkAndProcessKey) // Controlla e aggiorna la chiave per ogni TPP
                .doOnTerminate(() -> log.info("Verifica completata"))  // Messaggio quando termina la verifica
                .subscribe();
    }
    private void checkAndProcessKey(Tpp tpp) {
        // Step 1: Verifica se la chiave è in scadenza su Azure Key Vault
        azureEncryptService.isKeyExpiring(tpp.getTppId())
                .flatMap(isExpiring -> {
                    log.info("Chiave in scadenza per tppId: {}",tpp.getTppId());
                    if (Boolean.TRUE.equals(isExpiring)) {
                        // Step 2: Imposta il lock a true per prevenire l'accesso
                        tpp.setLock(true);
                        tppRepository.save(tpp);
                        // Step 3: Decriptazione de dati con la chiave attuale
                        azureEncryptService.keyDecrypt(tpp.getTokenSection(),tpp.getTppId());
                        // Step 4: Creazione della nuova chiave e criptazione dei dati
                        KeyVaultKey newKey = azureEncryptService.createRsaKey(tpp.getTppId());
                        // Step 6: Aggiorna il documento i dati criptati e rilascio il lock
                        azureEncryptService.keyEncrypt(tpp.getTokenSection(),newKey);
                        tpp.setLock(false);
                        tpp.setLastUpdateDate(LocalDateTime.now());
                        return tppRepository.save(tpp)
                                .doOnSuccess(updatedTpp -> log.info("Chiave aggiornata per tppId: {}",updatedTpp.getTppId()));
                    }
                    // Se la chiave non è in scadenza, non fare nulla
                    return Mono.empty();
                });
    }

}

