package it.gov.pagopa.tpp.dto.mapper;


import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.model.Tpp;
import org.springframework.stereotype.Service;

@Service
public class TppObjectToDTOMapper {

    public TppDTO map(Tpp tpp){
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
                .paymentButton(tpp.getPaymentButton())
                .agentDeepLinks(tpp.getAgentDeepLinks())
                .build();
    }
}
