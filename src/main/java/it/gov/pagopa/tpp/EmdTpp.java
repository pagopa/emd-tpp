package it.gov.pagopa.tpp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class EmdTpp {

	public static void main(String[] args) {
		SpringApplication.run(EmdTpp.class, args);
	}

}
