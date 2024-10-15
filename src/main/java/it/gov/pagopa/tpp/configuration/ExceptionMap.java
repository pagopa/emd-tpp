package it.gov.pagopa.tpp.configuration;


import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.tpp.constants.OnboardingTppConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Configuration
public class ExceptionMap {

    private final Map<String, Supplier<ClientException>> exceptions = new HashMap<>();

    public ExceptionMap() {
        exceptions.put(OnboardingTppConstants.ExceptionName.TPP_NOT_ONBOARDED, () ->
                new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        OnboardingTppConstants.ExceptionCode.TPP_NOT_ONBOARDED,
                        OnboardingTppConstants.ExceptionMessage.TPP_NOT_ONBOARDED
                )
        );

    }

    public RuntimeException getException(String exceptionKey) {
        if (exceptions.containsKey(exceptionKey)) {
            return exceptions.get(exceptionKey).get();
        } else {
            throw new IllegalArgumentException(String.format("Exception Name Not Found: %s", exceptionKey));
        }
    }

}
