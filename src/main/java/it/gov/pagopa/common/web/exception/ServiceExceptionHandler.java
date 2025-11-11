package it.gov.pagopa.common.web.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import it.gov.pagopa.common.web.dto.ErrorDTO;

import java.util.Map;

/**
 * Specialized exception handler for {@link ServiceException} instances with high precedence ordering.
 * <p>
 * This handler extends the global exception handling system by providing dedicated processing
 * for service layer exceptions. It operates with {@link Ordered#HIGHEST_PRECEDENCE} to ensure
 * that service exceptions are handled before falling back to the general exception handlers.
 * The handler supports both payload-based and standard error responses, with configurable
 * HTTP status code mapping for different service exception types.
 * <p>
 * The handler provides two distinct processing paths:
 * <ul>
 *   <li>Payload-based handling - When a {@link ServiceException} contains a
 *       {@link ServiceExceptionPayload}, the payload is directly serialized as the response body</li>
 *   <li>Standard handling - Service exceptions without payloads are transcoded
 *       to {@link ClientExceptionWithBody} and processed through the {@link ErrorManager}</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceExceptionHandler {
  /**
    * The error manager used for standard exception processing when no payload is present.
    */
  private final ErrorManager errorManager;

  /**
    * Mapping of service exception types to their corresponding HTTP status codes.
    */
  private final Map<Class<? extends ServiceException>, HttpStatus> transcodeMap;

  public ServiceExceptionHandler(ErrorManager errorManager, Map<Class<? extends ServiceException>, HttpStatus> transcodeMap) {
    this.errorManager = errorManager;
    this.transcodeMap = transcodeMap;
  }

  /**
    * Handles {@link ServiceException} instances with differentiated processing based on payload presence:
    * <ul>
    *   <li>If the exception contains a {@link ServiceExceptionPayload}, the payload is used
    *       directly as the response body</li>
    *   <li>If no payload is present, the exception is transcoded to a {@link ClientExceptionWithBody}
    *       and processed through the {@link ErrorManager}</li>
    * </ul>
    * <p>
    * The method returns a {@link ResponseEntity} to accommodate both
    * {@link ServiceExceptionPayload} instances and {@link ErrorDTO} responses from the error manager.
    *
    * @param error the service exception to handle
    * @return a {@link ResponseEntity} containing either:
    *         <ul>
    *           <li>The exception's payload as the response body (if payload is present)</li>
    *           <li>A standardized error response from the {@link ErrorManager} (if no payload)</li>
    *         </ul>
    */
  @SuppressWarnings("squid:S1452")
  @ExceptionHandler(ServiceException.class)
  protected ResponseEntity<? extends ServiceExceptionPayload> handleException(ServiceException error) {
    if (null != error.getPayload()) {
      return handleBodyProvidedException(error);
    }
    return errorManager.handleException(transcodeException(error));
  }

  /**
   * Transcodes a {@link ServiceException} to a {@link ClientExceptionWithBody} for standard processing.
   * <p>
   * This method maps service exceptions to client exceptions.
   * Unknown exception types are logged as warnings to aid in configuration management
   * and ensure that all expected service exception types are properly mapped.
   *
   * @param error the service exception to transcode
   * @return a {@link ClientExceptionWithBody} configured with appropriate HTTP status,
   *         error code, message, and debugging preferences
   */
  private ClientException transcodeException(ServiceException error) {
    HttpStatus httpStatus = transcodeMap.get(error.getClass());

    if (httpStatus == null) {
      log.warn("Unhandled exception: {}", error.getClass().getName());
      httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    return new ClientExceptionWithBody(httpStatus, error.getCode(), error.getMessage(), error.isPrintStackTrace(), error);
  }

  /**
   * Handles service exceptions that contain payload objects for direct response serialization.
   * <p>
   * This method processes service exceptions that provide their own response payload by:
   * <ul>
   *   <li>Transcoding the exception to determine the appropriate HTTP status code</li>
   *   <li>Logging the exception using the standard error manager logging mechanism</li>
   *   <li>Returning the payload directly as the JSON response body</li>
   * </ul>
   *
   * @param error the service exception containing a payload
   * @return a {@link ResponseEntity} with the exception's payload as the response body,
   *         appropriate HTTP status code, and JSON content type
   */
  private ResponseEntity<? extends ServiceExceptionPayload> handleBodyProvidedException(ServiceException error) {
    ClientException clientException = transcodeException(error);
    ErrorManager.logClientException(clientException);

    return ResponseEntity.status(clientException.getHttpStatus())
            .contentType(MediaType.APPLICATION_JSON)
            .body(error.getPayload());
  }
}