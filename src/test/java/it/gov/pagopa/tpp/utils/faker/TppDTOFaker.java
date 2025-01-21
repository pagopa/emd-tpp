package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;

import java.util.HashMap;

public class TppDTOFaker {

    private TppDTOFaker(){}

    public static TppDTO mockInstance(Boolean bias) {

        Contact contact = new Contact("name","number", "email");

        return TppDTO.builder()
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
                .paymentButton("#button")
                .agentDeepLinks(new HashMap<>() {{
                    put("agent", "link");
                }})
                .build();
    }

    public static TppDTO mockInstanceWithNoTppId(Boolean bias) {

        Contact contact = new Contact("name","number", "email");

        return TppDTO.builder()
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
                .tokenSection(TokenSectionFaker.mockInstance())
                .build();
    }

    public static TppDTO mockInstanceWithNoTokenSection(Boolean bias) {

        Contact contact = new Contact("name","number", "email");

        return TppDTO.builder()
                .tppId(("tppId"))
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
                .tokenSection(null)
                .build();
    }

}
