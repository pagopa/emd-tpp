package it.gov.pagopa.tpp.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Batch lookup result, separating found TPPs from identifiers that do not exist. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TppBatchResponseDTO {
    private List<TppBatchItemDTO> results;
    private List<String> missingTppIds;
}
