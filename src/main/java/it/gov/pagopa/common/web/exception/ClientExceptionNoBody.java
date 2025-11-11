package it.gov.pagopa.common.web.exception;

import org.springframework.http.HttpStatus;

/**
 * Specialized client exception that indicates HTTP responses should be sent without a body.
 * <p>
 * It is specifically designed for scenarios where only the HTTP status code needs to be 
 * communicated to the client, without any response body or error details.
 */
public class ClientExceptionNoBody extends ClientException {

  public ClientExceptionNoBody(HttpStatus httpStatus, String message) {
    super(httpStatus, message);
  }

  public ClientExceptionNoBody(HttpStatus httpStatus, String message, Throwable ex) {
    super(httpStatus, message, ex);
  }

  public ClientExceptionNoBody(HttpStatus httpStatus, String message, boolean printStackTrace,
      Throwable ex) {
    super(httpStatus, message, printStackTrace, ex);
  }
}

