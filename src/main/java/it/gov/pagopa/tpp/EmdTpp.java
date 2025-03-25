package it.gov.pagopa.tpp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
@EnableScheduling
public class EmdTpp {

	public static void main(String[] args) {
		SpringApplication.run(EmdTpp.class, args);
	}

}
