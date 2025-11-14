package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;

import java.util.HashMap;

public class TppDTOWithoutTokenSectionFaker {

    private TppDTOWithoutTokenSectionFaker(){}

    public static TppDTOWithoutTokenSection mockInstance(Boolean bias) {

        Contact contact = new Contact("name","number", "email");

        return TppDTOWithoutTokenSection.builder()
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
                .paymentButton("#button")
                .agentDeepLinks(new HashMap<>() {{
                    put("agent", "link");
                }})
                .isPaymentEnabled(bias)
                .build();
    }

    public static TppDTOWithoutTokenSection mockInstanceWithNoTppId(Boolean bias) {

        Contact contact = new Contact("name","number", "email");

        return TppDTOWithoutTokenSection.builder()
                .tppId(null)
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
                .paymentButton("#button")
                .agentDeepLinks(new HashMap<>() {{
                    put("agent", "link");
                }})
                .build();
    }

}
