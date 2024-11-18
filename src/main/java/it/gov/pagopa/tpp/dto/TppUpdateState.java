package it.gov.pagopa.tpp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class TppUpdateState {
    @NotNull
    private String tppId;
    @NotNull
    private Boolean state;
}
