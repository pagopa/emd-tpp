package it.gov.pagopa.tpp.model.mapper;

import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.model.Agent;
import it.gov.pagopa.tpp.model.DeepLink;
import it.gov.pagopa.tpp.model.Tpp;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
                .agentDeepLinks(convertAgentDeepLinkToStringMap(tppDTO.getAgentDeepLinks()))
                .build();
    }

    private HashMap<String, String> convertAgentDeepLinkToStringMap(HashMap<Agent, DeepLink> agentDeepLink) {
        HashMap<String, String> result = new HashMap<>();

        if (agentDeepLink != null) {
            for (Map.Entry<Agent, DeepLink> entry : agentDeepLink.entrySet()) {
                String agentValue = entry.getKey().getUserAgent();
                String deepLinkValue = entry.getValue().getLink();
                result.put(agentValue, deepLinkValue);
            }
        }

        return result;
    }
}
