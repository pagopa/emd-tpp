package it.gov.pagopa.tpp.dto;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.AgentDeepLink;
import it.gov.pagopa.tpp.model.Contact;
import it.gov.pagopa.tpp.model.TokenSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * Data Transfer Object representing a complete TPP entity with full configuration.
 */ 
@Data
@SuperBuilder
@NoArgsConstructor
public class TppDTO {

    private String tppId;

    @NotBlank(message = "Entity ID must not be blank")
    @Pattern(regexp = "^(\\d{11}|[A-Za-z0-9]{16})$", message = "Entity ID must be 11 digits or up to 16 alphanumeric characters")
    private String entityId;

    @NotBlank(message = "ID PSP must not be blank")
    private String idPsp;

    @NotBlank(message = "Business name must not be blank")
    private String businessName;

    @NotBlank(message = "Legal address must not be blank")
    private String legalAddress;

    @Pattern(regexp = "^(https?|ftp)://[^ /$.?#].[^ ]*$", message = "Message URL must be a valid URL")
    private String messageUrl;

    @Pattern(regexp = "^(https?|ftp)://[^ /$.?#].[^ ]*$", message = "Authentication URL must be a valid URL")
    private String authenticationUrl;

    @NotNull(message = "Authentication type must not be null")
    private AuthenticationType authenticationType;

    @NotNull(message = "Contact must not be null")
    private Contact contact;
    private Boolean state;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
    private TokenSection tokenSection;

    @NotNull(message = "pspDenomination must not be null")
    private String pspDenomination;

    @NotNull(message = "Agent Deep Link must not be null")
    private HashMap<String, AgentDeepLink> agentDeepLinks;

    @NotNull(message = "IsPaymentEnabled must not be null")
    private Boolean isPaymentEnabled;

    private String messageTemplate;
}
