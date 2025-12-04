package it.gov.pagopa.tpp.model;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents the Agent Deep Link field with fallback link and version details.
 */
@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
public class AgentDeepLink {

    private String fallBackLink;
    private HashMap<String, VersionDetails> versions;
    
}
