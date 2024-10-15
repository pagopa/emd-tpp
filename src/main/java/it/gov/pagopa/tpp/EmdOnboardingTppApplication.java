package it.gov.pagopa.tpp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class EmdOnboardingTppApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmdOnboardingTppApplication.class, args);
	}

}
