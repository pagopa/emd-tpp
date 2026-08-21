package it.gov.pagopa.tpp.dto.mapper;

import it.gov.pagopa.tpp.constants.TppConstants.SearchFields;
import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.model.Tpp;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
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
                .clientId(tpp.getClientId())
                .idPsp(tpp.getIdPsp())
                .legalAddress(tpp.getLegalAddress())
                .businessName(tpp.getBusinessName())
                .contact(tpp.getContact())
                .entityId(tpp.getEntityId())
                .creationDate(tpp.getCreationDate())
                .lastUpdateDate(tpp.getLastUpdateDate())
                .pspDenomination(tpp.getPspDenomination())
                .agentLinks(tpp.getAgentLinks())
                .isPaymentEnabled(tpp.getIsPaymentEnabled())
                .messageTemplate(templateToUse)
                .whitelistRecipient(tpp.getWhitelistRecipient())
                .build();
    }

    /**
     * Maps a {@link Tpp} domain object to a {@link TppDTOWithoutTokenSection}, populating only
     * the requested {@code fields} (plus {@code tppId}, which is always included as row
     * identifier). Used by the search projection to keep the response payload small, mirroring
     * the fields already projected out of MongoDB.
     *
     * @param tpp    the domain entity to selectively map
     * @param fields the set of field names (matching {@link SearchFields}) to populate
     * @return a new {@link TppDTOWithoutTokenSection} instance containing only the requested fields
     */
    public TppDTOWithoutTokenSection map(Tpp tpp, Set<String> fields) {
        TppDTOWithoutTokenSection.TppDTOWithoutTokenSectionBuilder<?, ?> builder = TppDTOWithoutTokenSection.builder()
                .tppId(tpp.getTppId());

        if (fields.contains(SearchFields.CLIENT_ID)) builder.clientId(tpp.getClientId());
        if (fields.contains(SearchFields.ENTITY_ID)) builder.entityId(tpp.getEntityId());
        if (fields.contains(SearchFields.ID_PSP)) builder.idPsp(tpp.getIdPsp());
        if (fields.contains(SearchFields.BUSINESS_NAME)) builder.businessName(tpp.getBusinessName());
        if (fields.contains(SearchFields.LEGAL_ADDRESS)) builder.legalAddress(tpp.getLegalAddress());
        if (fields.contains(SearchFields.MESSAGE_URL)) builder.messageUrl(tpp.getMessageUrl());
        if (fields.contains(SearchFields.AUTHENTICATION_URL)) builder.authenticationUrl(tpp.getAuthenticationUrl());
        if (fields.contains(SearchFields.AUTHENTICATION_TYPE)) builder.authenticationType(tpp.getAuthenticationType());
        if (fields.contains(SearchFields.CONTACT)) builder.contact(tpp.getContact());
        if (fields.contains(SearchFields.STATE)) builder.state(tpp.getState());
        if (fields.contains(SearchFields.CREATION_DATE)) builder.creationDate(tpp.getCreationDate());
        if (fields.contains(SearchFields.LAST_UPDATE_DATE)) builder.lastUpdateDate(tpp.getLastUpdateDate());
        if (fields.contains(SearchFields.PSP_DENOMINATION)) builder.pspDenomination(tpp.getPspDenomination());
        if (fields.contains(SearchFields.AGENT_LINKS)) builder.agentLinks(tpp.getAgentLinks());
        if (fields.contains(SearchFields.IS_PAYMENT_ENABLED)) builder.isPaymentEnabled(tpp.getIsPaymentEnabled());
        if (fields.contains(SearchFields.WHITELIST_RECIPIENT)) builder.whitelistRecipient(tpp.getWhitelistRecipient());
        if (fields.contains(SearchFields.MESSAGE_TEMPLATE)) {
            boolean hasCustomTemplate = StringUtils.hasText(tpp.getMessageTemplate());
            builder.messageTemplate(hasCustomTemplate ? tpp.getMessageTemplate() : defaultTemplateContent);
        }

        return builder.build();
    }
}
