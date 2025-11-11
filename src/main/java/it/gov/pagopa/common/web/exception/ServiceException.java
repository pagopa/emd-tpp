package it.gov.pagopa.common.web.exception;

import lombok.Getter;

/**
 * Custom runtime exception class for application-wide error handling.
 * <p>
 * It provides structured error information including error codes, optional payloads, and configurable
 * logging behavior.
 */
@Getter
public class ServiceException extends RuntimeException {
  private final String code;
  private final boolean printStackTrace;
  private final ServiceExceptionPayload payload;

  public ServiceException(String code, String message) {
    this(code, message, null);
  }
  public ServiceException(String code, String message, ServiceExceptionPayload payload) {
    this(code, message, payload, false, null);
  }

  public ServiceException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
    super(message, ex);
    this.code = code;
    this.printStackTrace = printStackTrace;
    this.payload = payload;
  }

}
