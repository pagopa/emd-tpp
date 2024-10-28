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
                .messageUrl(tppDTO.getMessageUrl())
                .authenticationUrl(tppDTO.getAuthenticationUrl())
                .authenticationType(tppDTO.getAuthenticationType())
                .businessName(tppDTO.getBusinessName())
                .contact(tppDTO.getContact())
                .entityId(tppDTO.getEntityId())
                .build();
    }
}
