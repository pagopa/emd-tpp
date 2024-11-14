package it.gov.pagopa.tpp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class TppUpdateState {
    @NotNull
    private String tppId;
    @NotNull
    private Boolean state;
}
