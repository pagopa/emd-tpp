package it.gov.pagopa.tpp.dto;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.AgentLink;
import it.gov.pagopa.tpp.model.Contact;
import jakarta.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Data Transfer Object for partial updates of a TPP entity (without token section).
 * Only non-null fields present in the request body will be applied to the existing TPP.
 * The {@code tppId} field is mandatory and must always be provided.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class TppDTOPatch {


    @Pattern(regexp = "^(\\d{11}|[A-Za-z0-9]{16})$", message = "Entity ID must be 11 digits or up to 16 alphanumeric characters")
    private String entityId;

    private String clientId;

    private String idPsp;

    private String businessName;

    private String legalAddress;

    @Pattern(regexp = "^(https?|ftp)://[^ /$.?#].[^ ]*$", message = "Message URL must be a valid URL")
    private String messageUrl;

    @Pattern(regexp = "^(https?|ftp)://[^ /$.?#].[^ ]*$", message = "Authentication URL must be a valid URL")
    private String authenticationUrl;

    private AuthenticationType authenticationType;

    private Contact contact;

    private String pspDenomination;

    private HashMap<String, AgentLink> agentLinks;

    private Boolean isPaymentEnabled;

    private String messageTemplate;

    private List<String> whitelistRecipient;
}

