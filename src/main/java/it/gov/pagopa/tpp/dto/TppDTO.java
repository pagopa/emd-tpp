package it.gov.pagopa.tpp.dto;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class TppDTO {
    private String tppId;
    private String entityId;
    private String businessName;
    private String messageUrl;
    private String authenticationUrl;
    private AuthenticationType authenticationType;
    private Contact contact;
    private Boolean state;

}
