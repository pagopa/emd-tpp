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

/**
 * Configuration class that provides centralized exception mapping and creation for TPP operations.
 * <p>
 * This class implements a factory pattern for creating standardized {@link ClientException} instances
 * based on predefined exception keys. 

 * <p>
 * Currently supported TPP exception scenarios:
 * <ul>
 *   <li>TPP_NOT_ONBOARDED - Returns HTTP 404 when a TPP is not found or not onboarded</li>
 *   <li>TPP_ALREADY_ONBOARDED - Returns HTTP 403 when attempting to onboard an already registered TPP</li>
 * </ul>
 */
@Configuration
@Slf4j
public class ExceptionMap {
    /**
     * This map associates string-based exception identifiers with {@link Function} instances
     * that create appropriately configured {@link ClientException} objects. Each function
     * takes a custom message as input and returns a configured exception with the
     * correct HTTP status code, error code, and message.
     */
    private final Map<String, Function<String, ClientException>> exceptions = new HashMap<>();

    /**
     * This constructor sets up the mapping between exception names and their corresponding
     * factory functions. 
     * <p>
     * Registered exceptions:
     * <ul>
     *   <li>TPP_NOT_ONBOARDED - HTTP 404 with NOT_FOUND status for missing TPPs</li>
     *   <li>TPP_ALREADY_ONBOARDED - HTTP 403 with FORBIDDEN status for duplicate onboarding</li>
     * </ul>
     */
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
    /**
     * Creates and returns a runtime exception based on the specified exception key and message.
     * <p>
     * It looks up the appropriate exception factory function based
     * on the provided key and applies the custom message to create a fully configured
     * {@link ClientException} instance.
     * <p>
     * If the exception key is not found in the registry, the method logs an error and
     * returns a generic {@link RuntimeException} as a fallback. 
     *
     * @param exceptionKey the predefined exception identifier corresponding to a specific
     *                    TPP business scenario
     * @param message the custom error message to include in the exception
     * @return a {@link RuntimeException} instance, which will be:
     *         <ul>
     *           <li>A properly configured {@link ClientExceptionWithBody} if the key is recognized</li>
     *           <li>A generic {@link RuntimeException} if the key is not found in the registry</li>
     *         </ul>
     */
    public RuntimeException throwException(String exceptionKey, String message) {
        if (exceptions.containsKey(exceptionKey)) {
            return exceptions.get(exceptionKey).apply(message);
        } else {
            log.error("[EMP-TPP][EXCEPTION-MAP] Exception Name Not Found: {}", exceptionKey);
            return  new RuntimeException();
        }
    }

}

