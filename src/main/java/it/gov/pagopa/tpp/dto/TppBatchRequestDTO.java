package it.gov.pagopa.tpp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request payload for resolving a page of TPP identifiers in one call. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TppBatchRequestDTO {

    public static final int MAX_IDS = 100;

    @NotEmpty
    @Size(max = MAX_IDS)
    private List<@NotBlank String> ids;
}
