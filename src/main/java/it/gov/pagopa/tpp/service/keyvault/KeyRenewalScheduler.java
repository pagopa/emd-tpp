package it.gov.pagopa.tpp.service.keyvault;

import com.azure.security.keyvault.keys.models.KeyVaultKey;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.repository.TppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@EnableScheduling
@Slf4j
public class KeyRenewalScheduler {

    private final TppRepository tppRepository;

    private final AzureEncryptService azureEncryptService;

    public KeyRenewalScheduler(TppRepository tppRepository, AzureEncryptService azureEncryptService) {
        this.tppRepository = tppRepository;
        this.azureEncryptService = azureEncryptService;
    }

    @Scheduled(fixedRate = 60000)
    public void renewExpiringKeys() {
        log.info("[Key-Renewal-Scheduler] Avvio rinnovo chiavi...");
        tppRepository.findAll()
                .doOnNext(this::checkAndProcessKey)
                .doOnTerminate(() -> log.info("[Key-Renewal-Scheduler][Renew-Expiring-Keys]Verifica completata"))
                .subscribe();
    }
    private void checkAndProcessKey(Tpp tpp) {
        azureEncryptService.isKeyExpiring(tpp.getTppId())
                .flatMap(isExpiring -> {
                    log.info("Chiave in scadenza per tppId: {}",tpp.getTppId());
                    if (Boolean.TRUE.equals(isExpiring)) {
                        tpp.setLock(true);
                        tppRepository.save(tpp);
                        azureEncryptService.keyDecrypt(tpp.getTokenSection(),tpp.getTppId());
                        KeyVaultKey newKey = azureEncryptService.createRsaKey(tpp.getTppId());
                        azureEncryptService.keyEncrypt(tpp.getTokenSection(),newKey);
                        tpp.setLock(false);
                        tpp.setLastUpdateDate(LocalDateTime.now());
                        return tppRepository.save(tpp)
                                .doOnSuccess(updatedTpp -> log.info("[Key-Renewal-Scheduler][Check-And-Process-Key] Chiave aggiornata per tppId: {}",updatedTpp.getTppId()));
                    }
                    return Mono.empty();
                });
    }

}

