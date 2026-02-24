package it.gov.pagopa.tpp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a single recipient ID for whitelist operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhitelistRecipientDTO {
    @NotNull
    private String recipientId;
}