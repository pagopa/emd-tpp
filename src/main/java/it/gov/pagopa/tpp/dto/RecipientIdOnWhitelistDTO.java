package it.gov.pagopa.tpp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecipientIdOnWhitelistDTO {

  @NotBlank(message = "recipientId must not be blank")
  private String recipientId;
}
