package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.dto.TppDTOPatch;
import it.gov.pagopa.tpp.model.Contact;

public class TppDTOPatchFaker {

    private TppDTOPatchFaker() {}

    /** Patch con TUTTI i campi valorizzati (tranne tppId, che viene dal path). */
    public static TppDTOPatch mockInstance() {
        return TppDTOPatch.builder()
                .businessName("updatedBusinessName")
                .messageUrl("https://www.updatedMessageUrl.it")
                .authenticationUrl("https://www.updatedAuthUrl.it")
                .legalAddress("updatedLegalAddress")
                .contact(new Contact("updatedName", "updatedNumber", "updatedEmail"))
                .pspDenomination("updatedPspDenomination")
                .isPaymentEnabled(true)
                .messageTemplate("{\"updatedKey\": \"updatedValue\"}")
                .clientId("updatedClientId")
                .build();
    }

    /** Patch con SOLO businessName valorizzato — tutti gli altri campi null (clientId incluso). */
    public static TppDTOPatch mockPartialInstance() {
        return TppDTOPatch.builder()
                .businessName("onlyBusinessNameUpdated")
                .build();
    }

    /** Patch con SOLO clientId valorizzato — per testare l'edge case legacy. */
    public static TppDTOPatch mockClientIdOnlyInstance() {
        return TppDTOPatch.builder()
                .clientId("newLegacyClientId")
                .build();
    }
}

