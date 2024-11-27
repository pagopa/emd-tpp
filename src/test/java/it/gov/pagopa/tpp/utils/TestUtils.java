package it.gov.pagopa.tpp.utils;

import it.gov.pagopa.tpp.dto.TokenSectionDTO;
import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.dto.TppIdList;
import it.gov.pagopa.tpp.utils.faker.TokenSectionDTOFaker;
import it.gov.pagopa.tpp.utils.faker.TppDTOFaker;
import it.gov.pagopa.tpp.utils.faker.TppDTOWithoutTokenSectionFaker;
import it.gov.pagopa.tpp.utils.faker.TppFaker;
import it.gov.pagopa.tpp.model.Tpp;

import java.util.List;

public class TestUtils {

    public static final TppDTO MOCK_TPP_DTO = TppDTOFaker.mockInstance(true);
    public static final TppDTOWithoutTokenSection MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION = TppDTOWithoutTokenSectionFaker.mockInstance(true);
    public static final TppDTOWithoutTokenSection MOCK_TPP_DTO_WITHOUT_TOKEN_SECTION_NO_ID = TppDTOWithoutTokenSectionFaker.mockInstanceWithNoTppId(true);
    public static final TokenSectionDTO MOCK_TOKEN_SECTION_DTO = TokenSectionDTOFaker.mockInstance();
    public static final Tpp MOCK_TPP = TppFaker.mockInstance(true);
    public static final List<TppDTO> MOCK_TPP_DTO_LIST = List.of(MOCK_TPP_DTO);
    public static final List<Tpp> MOCK_TPP_LIST = List.of(TppFaker.mockInstance(true));
    public static final List<String> MOCK_TPP_ID_STRING_LIST = List.of(MOCK_TPP_DTO.getTppId());
    public static final TppIdList MOCK_TPP_ID_LIST = new TppIdList(MOCK_TPP_ID_STRING_LIST);
    public static final String MOCK_WRONG_ID = "wrong";

}
