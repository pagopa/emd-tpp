package it.gov.pagopa.tpp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * In this class we can find the details for each version of the agent deep link
 */
@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
public class VersionDetails {
    private String link;
}
