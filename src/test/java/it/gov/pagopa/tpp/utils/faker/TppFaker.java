package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;
import it.gov.pagopa.tpp.model.Tpp;

import java.util.HashMap;

public class TppFaker {

    private TppFaker(){}
    public static Tpp mockInstance(Boolean bias){

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
                .tokenSection(TokenSectionFaker.mockInstance())
                .pspDenomination("#button")
                .agentDeepLinks(new HashMap<>() {{
                    put("agent", "link");
                }})
                .isPaymentEnabled(bias)
                .build();
    }
}
