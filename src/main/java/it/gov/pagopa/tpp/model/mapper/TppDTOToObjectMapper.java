package it.gov.pagopa.tpp.model.mapper;

import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.model.Tpp;
import org.springframework.stereotype.Service;

@Service
public class TppDTOToObjectMapper {

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
                .paymentButton(tppDTO.getPaymentButton())
                .agentDeepLink(tppDTO.getAgentDeepLink())
                .build();
    }
}
