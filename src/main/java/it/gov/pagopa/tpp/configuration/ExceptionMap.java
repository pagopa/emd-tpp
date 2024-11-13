package it.gov.pagopa.tpp.configuration;


import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.tpp.constants.TppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@Slf4j
public class ExceptionMap {

    private final Map<String, Function<String, ClientException>> exceptions = new HashMap<>();

    public ExceptionMap() {
        exceptions.put(TppConstants.ExceptionName.TPP_NOT_ONBOARDED, message ->
                new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        TppConstants.ExceptionCode.TPP_NOT_ONBOARDED,
                        message
                )
        );

        exceptions.put(TppConstants.ExceptionName.TPP_ALREADY_ONBOARDED, message ->
                new ClientExceptionWithBody(
                        HttpStatus.FORBIDDEN,
                        TppConstants.ExceptionCode.TPP_ALREADY_ONBOARDED,
                        message
                )
        );
    }

    public RuntimeException throwException(String exceptionKey, String message) {
        if (exceptions.containsKey(exceptionKey)) {
            return exceptions.get(exceptionKey).apply(message);
        } else {
            log.error("[EMP-TPP][EXCEPTION-MAP] Exception Name Not Found: {}", exceptionKey);
            return  new RuntimeException();
        }
    }

}

