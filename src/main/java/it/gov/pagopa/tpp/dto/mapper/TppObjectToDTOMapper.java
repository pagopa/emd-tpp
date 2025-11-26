package it.gov.pagopa.tpp.dto.mapper;


import it.gov.pagopa.tpp.dto.TppDTO;
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
 * Service class responsible for mapping {@link Tpp} domain objects to 
 * {@link TppDTO} data transfer objects.
 */ 
@Service
@Slf4j
public class TppObjectToDTOMapper {
    @Value("classpath:templates/default_message.ftl")
    private Resource defaultTemplateResource;

    private String defaultTemplateContent;

    @PostConstruct
    public void init() {
        try {
            log.info("[TPP-MAPPER] Default message template loaded from file: {}", defaultTemplateResource.getFilename());
            this.defaultTemplateContent = StreamUtils.copyToString(
                defaultTemplateResource.getInputStream(),
                StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException("[TPP-MAPPER] Error while loading message template default file", e);
        }
    }

    /**
     * Maps a complete {@link Tpp} domain object to its corresponding {@link TppDTO} representation.
     * 
     * @param tpp the domain entity containing complete TPP information to be mapped
     * @return a new {@link TppDTO} instance containing all mapped properties from the input
     *         domain object
     */
    public TppDTO map(Tpp tpp){
        boolean hasCustomTemplate = StringUtils.hasText(tpp.getMessageTemplate());

        String templateToUse = hasCustomTemplate
            ? tpp.getMessageTemplate()
            : defaultTemplateContent;

        log.debug("[TPP-MAPPER][MAP] EntityId: {} - TppId: {}. Template Source: {}",
            tpp.getEntityId(),
            tpp.getTppId(),
            hasCustomTemplate ? "CUSTOM_DB" : "DEFAULT_FILE"
        );

        return TppDTO.builder()
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
            .tokenSection(tpp.getTokenSection())
            .pspDenomination(tpp.getPspDenomination())
            .agentDeepLinks(tpp.getAgentDeepLinks())
            .isPaymentEnabled(tpp.getIsPaymentEnabled())
            .messageTemplate(templateToUse)
            .build();
    }
}
