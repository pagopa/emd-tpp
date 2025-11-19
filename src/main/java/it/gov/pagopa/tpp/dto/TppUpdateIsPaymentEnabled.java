package it.gov.pagopa.tpp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Data Transfer Object for updating the payment authorization of a TPP.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class TppUpdateIsPaymentEnabled {
    @NotNull
    private Boolean isPaymentEnabled;
}