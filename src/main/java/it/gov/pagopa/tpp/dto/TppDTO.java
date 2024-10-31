package it.gov.pagopa.tpp.dto;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
public class TppDTO {
    @NotBlank(message = "TPP ID must not be blank")
    private String tppId;

    @NotBlank(message = "Entity ID must not be blank")
    @Pattern(regexp = "^(\\d{11}|[A-Za-z0-9]{1,16})$",
            message = "Entity ID must be 11 digits or up to 16 alphanumeric characters")
    private String entityId;
    private String idPsp;
    private String businessName;
    private String legalAddress;

    @Pattern(regexp = "^(https?|ftp)://[^ /$.?#].[^ ]*$",
            message = "Message URL must be a valid URL")
    private String messageUrl;

    @Pattern(regexp = "^(https?|ftp)://[^ /$.?#].[^ ]*$",
            message = "Authentication URL must be a valid URL")
    private String authenticationUrl;
    private AuthenticationType authenticationType;
    private Contact contact;
    private Boolean state;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
}
