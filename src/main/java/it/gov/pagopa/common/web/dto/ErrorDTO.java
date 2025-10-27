package it.gov.pagopa.common.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing an error response payload.
 * <p>
 * This class encapsulates error information that is returned to clients
 * when exceptions or error conditions occur during service operations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
public class ErrorDTO implements ServiceExceptionPayload {

  @NotBlank
  private String code;
  @NotBlank
  private String message;
}
