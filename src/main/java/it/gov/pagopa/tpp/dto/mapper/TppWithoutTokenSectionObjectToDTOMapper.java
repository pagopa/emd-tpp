package it.gov.pagopa.tpp.dto.mapper;

import it.gov.pagopa.tpp.dto.TppDTOWithoutTokenSection;
import it.gov.pagopa.tpp.model.Tpp;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for mapping {@link Tpp} domain objects to {@link TppDTOWithoutTokenSection} 
 * data transfer objects.
 */ 
@Service
public class TppWithoutTokenSectionObjectToDTOMapper {

    /**
     * Maps a {@link Tpp} domain object to its corresponding {@link TppDTOWithoutTokenSection} 
     * representation.
     * 
     * @param tpp the domain entity containing complete TPP information to be selectively mapped
     * @return a new {@link TppDTOWithoutTokenSection} instance containing all 
     *         properties from the input domain object
     */
    public TppDTOWithoutTokenSection map(Tpp tpp){
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
                .paymentButton(tpp.getPaymentButton())
                .agentDeepLinks(tpp.getAgentDeepLinks())
                .build();
    }
}
