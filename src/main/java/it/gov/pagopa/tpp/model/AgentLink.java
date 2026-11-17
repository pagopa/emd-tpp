package it.gov.pagopa.tpp.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents the Agent Link field with fallback link and version details.
 * <p>
 * The {@code versions} map keys are persisted as MongoDB/Cosmos DB field names, which cannot
 * contain a dot ({@code .}). A dot in a version key is therefore rejected as a client error
 * ({@code 400}) at request binding time via the {@link Pattern} constraint on the map key, instead
 * of failing later as an internal error during the database write.
 */
@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
public class AgentLink {

    private String fallBackLink;

    @Valid
    private HashMap<@Pattern(regexp = "^[^.]*$", message = "Agent link version keys must not contain a dot ('.')") String, VersionDetails> versions;
    
}
