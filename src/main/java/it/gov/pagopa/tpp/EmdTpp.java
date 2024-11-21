package it.gov.pagopa.tpp;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.azure.security.keyvault.keys.models.CreateRsaKeyOptions;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

@Slf4j
@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class EmdTpp {

	public static void main(String[] args) {
		SpringApplication.run(EmdTpp.class, args);

		KeyClient keyClient = new KeyClientBuilder()
				.vaultUrl("https://cstar-d-weu-mil-kv.vault.azure.net")
				.credential(new DefaultAzureCredentialBuilder().build())
				.buildClient();

		KeyVaultKey rsaKey = keyClient.createRsaKey( new CreateRsaKeyOptions("1234")
				.setExpiresOn(OffsetDateTime.now().plusYears(1))
				.setKeySize(2048));

		CryptographyClient cryptographyClient = new CryptographyClientBuilder()
				.credential(new DefaultAzureCredentialBuilder().build())
				.keyIdentifier(rsaKey.getId())
				.buildClient();
		String stringaCoded = Base64.getEncoder().encodeToString(cryptographyClient.encrypt(EncryptionAlgorithm.RSA_OAEP_256, "ciao".getBytes(StandardCharsets.UTF_8)).getCipherText());
		byte[] decryptedValue =cryptographyClient.decrypt(EncryptionAlgorithm.RSA_OAEP_256, Base64.getDecoder().decode(stringaCoded)).getPlainText();

		log.info("Codificata: "+stringaCoded);
		log.info("Decodificata: "+new String(decryptedValue));
	}

}
