package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.AgentLink;
import it.gov.pagopa.tpp.model.Contact;
import it.gov.pagopa.tpp.model.Tpp;
import it.gov.pagopa.tpp.model.VersionDetails;

import java.lang.Runtime.Version;
import java.util.HashMap;

public class TppFaker {

    private TppFaker(){}
    public static Tpp mockInstance(Boolean bias){
        return mockInstance("tppId", bias);
    }

    public static Tpp mockInstance(String tppId, Boolean state){

        Contact contact = new Contact("name","number", "email");
        VersionDetails versionDetails = new VersionDetails("linkVersion");
        AgentLink agentLink = new AgentLink("ios", new HashMap<>() {{
            put("v1", versionDetails);
        }});

        return Tpp.builder()
                .id("id")
                .tppId(tppId)
                .messageUrl("https://wwwmessageUrl.it")
                .authenticationUrl("https://www.AuthenticationUrl.it")
                .idPsp("idPsp")
                .legalAddress("legalAddress")
                .authenticationType(AuthenticationType.OAUTH2)
                .state(state)
                .entityId("entityId01234567")
                .businessName("businessName")
                .contact(contact)
                .lastUpdateDate(null)
                .creationDate(null)
                .tokenSection(TokenSectionFaker.mockInstance())
                .pspDenomination("#button")
                .agentLinks(new HashMap<>() {{
                    put("agent", agentLink);
                }})
                .messageTemplate("{\"testKey\": ${associatedPayment???then(associatedPayment?c, 'null')}")
                .isPaymentEnabled(state)
                .build();
    }
}
