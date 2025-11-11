package it.gov.pagopa.common.web.exception;

import java.io.Serializable;

/**
 * Marker interface for objects that can be used as payload in {@link ServiceException} instances.
 * <p>
 * This interface extends {@link Serializable} and serves as a contract for objects that can be
 * carried within ServiceExceptions to provide additional context or structured error information.
 */
public interface ServiceExceptionPayload extends Serializable{

}
