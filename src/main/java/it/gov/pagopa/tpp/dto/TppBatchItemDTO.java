package it.gov.pagopa.tpp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Minimal TPP identity returned by the batch endpoint. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TppBatchItemDTO {
    private String tppId;
    private String businessName;
    private String entityId;
}
