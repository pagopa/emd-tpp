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
                .build();
    }

    /** Patch con SOLO businessName valorizzato — tutti gli altri campi null. */
    public static TppDTOPatch mockPartialInstance() {
        return TppDTOPatch.builder()
                .businessName("onlyBusinessNameUpdated")
                .build();
    }
}

