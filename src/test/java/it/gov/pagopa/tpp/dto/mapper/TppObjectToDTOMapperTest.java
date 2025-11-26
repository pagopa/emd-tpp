package it.gov.pagopa.tpp.dto.mapper;

import it.gov.pagopa.tpp.model.Tpp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TppObjectToDTOMapper.class)
class TppObjectToDTOMapperTest {

    @Autowired
    private TppObjectToDTOMapper mapper;

    @Test
    void whenTemplateIsNull_thenLoadFromFile() {
        Tpp tpp = new Tpp();
        tpp.setMessageTemplate(null);

        var dto = mapper.map(tpp);

        Assertions.assertNotNull(dto.getMessageTemplate());
        Assertions.assertTrue(dto.getMessageTemplate().contains("messageId"), "Il template di default dovrebbe contenere 'messageId'");
    }

    @Test
    void whenTemplateIsNotNull_thenReturnTemplate() {
        Tpp tpp = new Tpp();
        tpp.setMessageTemplate("""
            {"newTestKey": "${msgDescription}"}
        """);

        var dto = mapper.map(tpp);

        Assertions.assertNotNull(dto.getMessageTemplate());
        Assertions.assertTrue(dto.getMessageTemplate().contains("newTestKey"), "Il template di default dovrebbe contenere 'newTestKey'");
    }
}