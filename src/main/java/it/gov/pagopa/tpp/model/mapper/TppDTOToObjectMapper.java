package it.gov.pagopa.tpp.model.mapper;

import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.model.Tpp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct; // Usa javax.annotation se sei su Spring Boot < 3
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service class responsible for mapping {@link TppDTO} 
 * data transfer objects to {@link Tpp} domain objects.
 */
@Service
public class TppDTOToObjectMapper {

    /**
     * Maps a {@link TppDTO} to its corresponding {@link Tpp} domain object with business rule application.
     * 
     * @param tppDTO the DTO containing complete TPP information to be mapped
     * @return a new {@link Tpp} domain object containing all mapped properties from the input DTO
     */
    public Tpp map(TppDTO tppDTO){

        return Tpp.builder()
                .state(true)
                .tppId(tppDTO.getTppId())
                .idPsp(tppDTO.getIdPsp())
                .messageUrl(tppDTO.getMessageUrl())
                .legalAddress(tppDTO.getLegalAddress())
                .authenticationUrl(tppDTO.getAuthenticationUrl())
                .authenticationType(tppDTO.getAuthenticationType())
                .businessName(tppDTO.getBusinessName())
                .contact(tppDTO.getContact())
                .entityId(tppDTO.getEntityId())
                .tokenSection(tppDTO.getTokenSection())
                .pspDenomination(tppDTO.getPspDenomination())
                .agentDeepLinks(tppDTO.getAgentDeepLinks())
                .isPaymentEnabled(tppDTO.getIsPaymentEnabled())
                .messageTemplate(tppDTO.getMessageTemplate())
                .build();
    }

}
