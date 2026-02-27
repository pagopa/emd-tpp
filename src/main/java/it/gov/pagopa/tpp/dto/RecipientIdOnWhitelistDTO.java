package it.gov.pagopa.tpp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipientIdOnWhitelistDTO {

  @NotBlank
  private String recipientId;
}
