package it.gov.pagopa.tpp.utils;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.dto.TppIdList;
import it.gov.pagopa.tpp.dto.TppUpdateIsPaymentEnabled;
import it.gov.pagopa.tpp.model.TokenSection;
import it.gov.pagopa.tpp.utils.faker.*;
import it.gov.pagopa.tpp.model.Tpp;

import java.util.List;

public class TestUtils {
    public static final String MOCK_WRONG_ID = "wrong";


    public static TppDTO getMockTppDto() {
        return TppDTOFaker.mockInstance(true);
    }

    public static TppDTO getMockTppDtoNoTokenSection() {
        return TppDTOFaker.mockInstanceWithNoTokenSection(true);
    }

    public static TppDTOWithoutTokenSection getMockTppDtoWithoutTokenSection() {
        return TppDTOWithoutTokenSectionFaker.mockInstance(true);
    }

    public static TppDTOWithoutTokenSection getMockTppDtoWithoutTokenSectionNoId() {
        return TppDTOWithoutTokenSectionFaker.mockInstanceWithNoTppId(true);
    }

    public static TokenSectionDTO getMockTokenSectionDto() {
        return TokenSectionDTOFaker.mockInstance();
    }

    public static TokenSection getMockTokenSection() {
        return TokenSectionFaker.mockInstance();
    }

    public static Tpp getMockTpp() {
        return TppFaker.mockInstance(true);
    }

    public static List<TppDTO> getMockTppDtoList() {
        return List.of(getMockTppDto());
    }

    public static List<Tpp> getMockTppList() {
        return List.of(TppFaker.mockInstance(true));
    }

    public static List<String> getMockTppIdStringList() {
        return List.of(getMockTppDto().getTppId());
    }

    public static TppIdList getMockTppIdList() {
        return new TppIdList(getMockTppIdStringList());
    }

    public static TppUpdateIsPaymentEnabled getMockIsPaymentEnabled() {
        return TppUpdateIsPaymentEnabledFaker.mockInstance(true);
    }
}