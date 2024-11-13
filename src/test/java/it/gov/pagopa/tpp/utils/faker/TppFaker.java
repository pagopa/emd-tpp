package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;
import it.gov.pagopa.tpp.model.Tpp;

public class TppFaker {

    private TppFaker(){}
    public static Tpp mockInstance(Boolean bias){
        return Tpp.builder()
                .tppId("tppId")
                .messageUrl("https://wwwmessageUrl.it")
                .authenticationUrl("https://www.AuthenticationUrl.it")
                .idPsp("idPsp")
                .legalAddress("legalAddress")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(bias)
                .entityId("entityId01234567")
                .businessName("businessName")
                .contact(new Contact("name","number", "email"))
                .lastUpdateDate(null)
                .creationDate(null)
                .build();
    }
}
