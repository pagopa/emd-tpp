package it.gov.pagopa.tpp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for updating the operational state of a TPP.
 */
@Data
public class TppUpdateState {
    @NotNull
    private String tppId;
    @NotNull
    private Boolean state;
}
