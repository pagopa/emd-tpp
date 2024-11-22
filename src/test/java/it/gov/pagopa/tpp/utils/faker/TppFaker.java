package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;
import it.gov.pagopa.tpp.model.TokenSection;
import it.gov.pagopa.tpp.model.Tpp;

import java.util.HashMap;

public class TppFaker {

    private TppFaker(){}
    public static Tpp mockInstance(Boolean bias){

        TokenSection tokenSection = new TokenSection(
                "application/json",
                new HashMap<>() {{
                    put("pathKey1", "test");
                }},
                new HashMap<>() {{
                    put("bodyKey1", "test");
                }}
        );

        Contact contact = new Contact("name","number", "email");

        return Tpp.builder()
                .id("id")
                .tppId("tppId")
                .messageUrl("https://wwwmessageUrl.it")
                .authenticationUrl("https://www.AuthenticationUrl.it")
                .idPsp("idPsp")
                .legalAddress("legalAddress")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(bias)
                .entityId("entityId01234567")
                .businessName("businessName")
                .contact(contact)
                .lastUpdateDate(null)
                .creationDate(null)
                .tokenSection(tokenSection)
                .build();
    }
}
