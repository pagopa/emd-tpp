package it.gov.pagopa.tpp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for updating the payment authorization of a TPP.
 */
@Data
public class TppUpdateIsPaymentEnabled {
    @NotNull
    private Boolean isPaymentEnabled;
}