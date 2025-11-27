package it.gov.pagopa.tpp.dto.mapper;

import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.model.Tpp;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Service class responsible for mapping {@link Tpp} domain objects to {@link TppDTOWithoutTokenSection} 
 * data transfer objects.
 */ 
@Service
@Slf4j
public class TppWithoutTokenSectionObjectToDTOMapper {
    @Value("classpath:templates/default_message.ftl")
    private Resource defaultTemplateResource;

    private String defaultTemplateContent;

    @PostConstruct
    public void init() {
        try {
            log.info("[TPP-NOTOKEN-MAPPER] Default message template loaded from file: {}", defaultTemplateResource.getFilename());
            this.defaultTemplateContent = StreamUtils.copyToString(
                defaultTemplateResource.getInputStream(),
                StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException("[TPP-NOTOKEN-MAPPER] Error while loading message template default file", e);
        }
    }

    /**
     * Maps a {@link Tpp} domain object to its corresponding {@link TppDTOWithoutTokenSection} 
     * representation.
     * 
     * @param tpp the domain entity containing complete TPP information to be selectively mapped
     * @return a new {@link TppDTOWithoutTokenSection} instance containing all 
     *         properties from the input domain object
     */
    public TppDTOWithoutTokenSection map(Tpp tpp){
        boolean hasCustomTemplate = StringUtils.hasText(tpp.getMessageTemplate());

        String templateToUse = hasCustomTemplate
            ? tpp.getMessageTemplate()
            : defaultTemplateContent;

        log.debug("[TPP-NOTOKEN-MAPPER][MAP] EntityId: {} - TppId: {}. Template Source: {}",
            tpp.getEntityId(),
            tpp.getTppId(),
            hasCustomTemplate ? "CUSTOM_DB" : "DEFAULT_FILE"
        );

        return TppDTOWithoutTokenSection.builder()
                .state(tpp.getState())
                .messageUrl(tpp.getMessageUrl())
                .authenticationUrl(tpp.getAuthenticationUrl())
                .authenticationType(tpp.getAuthenticationType())
                .tppId(tpp.getTppId())
                .idPsp(tpp.getIdPsp())
                .legalAddress(tpp.getLegalAddress())
                .businessName(tpp.getBusinessName())
                .contact(tpp.getContact())
                .entityId(tpp.getEntityId())
                .creationDate(tpp.getCreationDate())
                .lastUpdateDate(tpp.getLastUpdateDate())
                .pspDenomination(tpp.getPspDenomination())
                .agentDeepLinks(tpp.getAgentDeepLinks())
                .isPaymentEnabled(tpp.getIsPaymentEnabled())
                .messageTemplate(templateToUse)
                .build();
    }
}
