package it.gov.pagopa.common.web.exception;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.MissingRequestValueException;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Specialized exception handler for validation and HTTP request-related exceptions with high precedence.
 * <p>
 * This handler operates with {@link Ordered#HIGHEST_PRECEDENCE} to ensure that validation and
 * HTTP-specific exceptions are processed before falling back to more general exception handlers.
 * <p>
 * The handler supports the following exception types:
 * <ul>
 *   <li>{@link WebExchangeBindException} - Request body validation failures with detailed field errors</li>
 *   <li>{@link MissingRequestValueException} - Missing required request parameters or headers</li>
 *   <li>{@link NoResourceFoundException} - Static resource not found errors</li>
 *   <li>{@link MethodNotAllowedException} - Unsupported HTTP method errors</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

    private final ErrorDTO templateValidationErrorDTO;

    /**
     * Constructs a new ValidationExceptionHandler with an optional template error DTO.
     * <p>
     * If no template is provided, a default error DTO with code "INVALID_REQUEST"
     * and message "Invalid request" will be created. The template determines the
     * error code used across all validation error responses.
     *
     * @param templateValidationErrorDTO the template error DTO to use for validation errors
     */
    public ValidationExceptionHandler(@Nullable ErrorDTO templateValidationErrorDTO) {
        this.templateValidationErrorDTO = Optional.ofNullable(templateValidationErrorDTO)
                .orElse(new ErrorDTO("INVALID_REQUEST", "Invalid request"));
    }

    /**
     * Handles {@link WebExchangeBindException} for request body validation failures.
     * <p>
     * It extracts all field validation errors and formats them into a message.
     *
     * @param e the web exchange bind exception containing validation errors
     * @return an {@link ErrorDTO} with the template error code and a detailed message
     */
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleWebExchangeBindException(
            WebExchangeBindException e) {

        String message = e.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return String.format("[%s]: %s", fieldName, errorMessage);
                }).collect(Collectors.joining("; "));

        log.info("A WebExchangeBindException occurred : HttpStatus 400 - {}", message);
        log.debug("Something went wrong while validating http request", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), message);
    }

    /**
     * Handles {@link MissingRequestValueException} for missing required request parameters or headers.
     * <p>
     * It provides a standardized error response for these common client-side errors.
     *
     * @param e the missing request value exception
     * @return an {@link ErrorDTO} with the template error code and a generic message
     */
    @ExceptionHandler(MissingRequestValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleMissingRequestValueException(MissingRequestValueException e) {
        log.info("A MissingRequestValueException occurred : HttpStatus 400 - Something went wrong due to a missing request value");
        log.debug("Something went wrong due to a missing request value", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), "Something went wrong due to a missing request value");
    }

    /**
     * Handles {@link NoResourceFoundException} for missing static resources.
     * <p>
     * This method processes exceptions that occur when clients request static resources that do not exist on the server.
     * It provides an appropriate error messaging for resource-specific failures.
     *
     * @param e the no resource found exception
     * @return an {@link ErrorDTO} with the template error code and a message
     *         indicating a missing static resource
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleNoResourceFoundException(NoResourceFoundException e) {

        log.info("A NoResourceFoundException occurred : HttpStatus 400 - Something went wrong due to a missing static resource");
        log.debug("Something went wrong due to a missing static resource", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), "Something went wrong due to a missing static resource");
    }

    /**
     * Handles {@link MethodNotAllowedException} for unsupported HTTP methods.
     * <p>
     * This method processes exceptions that occur when clients attempt to access
     * endpoints using HTTP methods that are not supported by the target endpoint.
     *
     * @param e the method not allowed exception
     * @return an {@link ErrorDTO} with the template error code and a message
     *         indicating that the request method is not supported
     */
    @ExceptionHandler(MethodNotAllowedException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleMethodNotAllowedException(MethodNotAllowedException e) {

        log.info("A MethodNotAllowedException occurred : HttpStatus 405 - Request is not supported");
        log.debug("Something went wrong due to a request not supported", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), "Request is not supported");
    }


}
