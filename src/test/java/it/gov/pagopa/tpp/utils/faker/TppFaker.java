package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.AgentDeepLink;
import it.gov.pagopa.tpp.model.Contact;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.model.VersionDetails;

import java.lang.Runtime.Version;
import java.util.HashMap;

public class TppFaker {

    private TppFaker(){}
    public static Tpp mockInstance(Boolean bias){

        Contact contact = new Contact("name","number", "email");
        VersionDetails versionDetails = new VersionDetails("linkVersion");
        AgentDeepLink agentDeepLink = new AgentDeepLink("ios", new HashMap<>() {{
            put("v1", versionDetails);
        }});

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
                .agentLinks(new HashMap<>() {{
                    put("agent", agentDeepLink);
                }})
                .messageTemplate("{\"testKey\": ${associatedPayment???then(associatedPayment?c, 'null')}")
                .isPaymentEnabled(bias)
                .build();
    }
}
