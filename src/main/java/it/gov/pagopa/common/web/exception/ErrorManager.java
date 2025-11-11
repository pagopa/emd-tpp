package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

/**
 * Global exception handler for REST API endpoints using Spring's {@code @RestControllerAdvice}.
 * <p>
 * This class provides centralized exception handling for all REST controllers in the application,
 * ensuring consistent error response formats and appropriate HTTP status codes. It handles
 * different types of exceptions and converts them into standardized JSON error responses
 * or status-only responses based on the exception type.
 * <p>
 * The error manager supports multiple exception types:
 * <ul>
 *   <li>{@link ClientExceptionNoBody} - Returns HTTP status only, without response body</li>
 *   <li>{@link ClientExceptionWithBody} - Returns structured error information in JSON format</li>
 *   <li>Generic {@link RuntimeException} - Returns default error response with HTTP 500</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class ErrorManager {
  private final ErrorDTO defaultErrorDTO;

  public ErrorManager(@Nullable ErrorDTO defaultErrorDTO) {
    this.defaultErrorDTO = Optional.ofNullable(defaultErrorDTO)
            .orElse(new ErrorDTO("Error", "Something gone wrong"));
  }
  
  /**
   * Handles all {@link RuntimeException} instances thrown by REST controllers.
    * <p>
    * This method provides differentiated handling based on the specific exception type:
    * <ul>
    *   <li>{@link ClientExceptionNoBody} - Returns only HTTP status code without response body</li>
    *   <li>{@link ClientExceptionWithBody} - Returns structured JSON error with code and message</li>
    *   <li>Other {@link RuntimeException} - Returns default error response with HTTP 500 status</li>
    * </ul>
    *
    * @param error the runtime exception to handle
    * @return a {@link ResponseEntity} containing either:
    *         <ul>
    *           <li>Empty body with appropriate HTTP status (for {@code ClientExceptionNoBody})</li>
    *           <li>JSON error response with structured error information (for {@code ClientExceptionWithBody})</li>
    *           <li>Default JSON error response with HTTP 500 status (for other exceptions)</li>
    *         </ul>
   */ 
  @ExceptionHandler(RuntimeException.class)
  protected ResponseEntity<ErrorDTO> handleException(RuntimeException error) {

    logClientException(error);

    if(error instanceof ClientExceptionNoBody clientExceptionNoBody){
      return ResponseEntity.status(clientExceptionNoBody.getHttpStatus()).build();
    }
    else {
      ErrorDTO errorDTO;
      HttpStatus httpStatus;
      if (error instanceof ClientExceptionWithBody clientExceptionWithBody){
        httpStatus=clientExceptionWithBody.getHttpStatus();
        errorDTO = new ErrorDTO(clientExceptionWithBody.getCode(),  error.getMessage());
      }
      else {
        httpStatus=HttpStatus.INTERNAL_SERVER_ERROR;
        errorDTO = defaultErrorDTO;
      }
      return ResponseEntity.status(httpStatus)
              .contentType(MediaType.APPLICATION_JSON)
              .body(errorDTO);
    }
  }

  /**
   * Provides logging for runtime exceptions with the appropriate logging level and detail. 
   * It unwraps {@link ServiceException} causes when present and adjusts logging behavior based on:
   * <ul>
   *   <li>Exception type</li>
   *   <li>Stack trace printing preference</li>
   *   <li>Presence of underlying causes</li>
   * </ul>
   * <p>
   * Logging levels:
   * <ul>
   *   <li>ERROR level - For system exceptions, client exceptions with stack trace enabled,
   *       or exceptions with underlying causes</li>
   *   <li>INFO level - For simple client exceptions without stack trace requirements</li>
   * </ul>
   *
   * @param error the runtime exception to log
  */
  public static void logClientException(RuntimeException error) {
    Throwable unwrappedException = error.getCause() instanceof ServiceException
            ? error.getCause()
            : error;

    String clientExceptionMessage = "";
    if(error instanceof ClientException clientException) {
      clientExceptionMessage = "HttpStatus %s - %s%s".formatted(
              clientException.getHttpStatus(),
              (clientException instanceof ClientExceptionWithBody clientExceptionWithBody) ? clientExceptionWithBody.getCode() + ": " : "",
              clientException.getMessage()
      );
    }

    if(!(error instanceof ClientException clientException) || clientException.isPrintStackTrace() || unwrappedException.getCause() != null){
      log.error("Something went wrong : {}", clientExceptionMessage, unwrappedException);
    } else {
      log.info("{}",clientExceptionMessage);
    }
  }

}
