package it.gov.pagopa.tpp.faker;

import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;

public class TppDTOFaker {

    private TppDTOFaker(){}

    public static TppDTO mockInstance(Boolean bias) {
        return TppDTO.builder()
                .tppId("tppId")
                .uri("uri")
                .messageUrl("messageUrl")
                .authenticationUrl("AuthenticationUrl")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(bias)
                .entityId("entityId")
                .businessName("businessName")
                .contact(new Contact("name","number", "email"))
                .build();
    }
}