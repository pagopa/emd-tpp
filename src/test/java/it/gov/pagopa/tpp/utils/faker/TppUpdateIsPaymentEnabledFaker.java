package it.gov.pagopa.tpp.utils.faker;

import it.gov.pagopa.tpp.dto.TppUpdateIsPaymentEnabled;

public class TppUpdateIsPaymentEnabledFaker {
    
    private TppUpdateIsPaymentEnabledFaker(){}

    public static TppUpdateIsPaymentEnabled mockInstance (Boolean bias){

        return TppUpdateIsPaymentEnabled.builder()
        .isPaymentEnabled(bias)
        .build();
    }
}
