package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Contact;
import it.gov.pagopa.tpp.model.TokenSection;

import java.util.HashMap;

public class TppDTOFaker {

    private TppDTOFaker() {
    }

    public static TppDTO mockInstance(Boolean bias) {

        TokenSection tokenSection = new TokenSection(
                "application/json",
                new HashMap<>() {{
                    put("pathKey1", "test");
                }},
                new HashMap<>() {{
                    put("bodyKey1", "test");
                }}
        );

        Contact contact = new Contact("name", "number", "email");

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
                .tokenSection(tokenSection)
                .build();
    }

    public static TppDTO mockInstanceWithNoTppId(Boolean bias) {

        Contact contact = new Contact("name", "number", "email");
        TokenSection tokenSection = new TokenSection(
                "application/json",
                new HashMap<>() {{
                    put("pathKey1", "pathValue1");
                }},
                new HashMap<>() {{
                    put("bodyKey1", "bodyValue1");
                }}
        );
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
                .tokenSection(tokenSection)
                .build();
    }

    public static TppDTO mockInstanceWithNoTokenSection(Boolean bias) {

        Contact contact = new Contact("name", "number", "email");

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