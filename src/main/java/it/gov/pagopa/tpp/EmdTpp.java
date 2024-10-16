package it.gov.pagopa.tpp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class EmdTpp {

	public static void main(String[] args) {
		SpringApplication.run(EmdTpp.class, args);
	}

}
