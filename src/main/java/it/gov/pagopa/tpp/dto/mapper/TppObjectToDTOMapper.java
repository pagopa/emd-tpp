package it.gov.pagopa.tpp.dto.mapper;


import it.gov.pagopa.tpp.dto.TppDTO;
import it.gov.pagopa.tpp.enums.AuthenticationType;
import it.gov.pagopa.tpp.model.Tpp;
import org.springframework.stereotype.Service;

@Service
public class TppObjectToDTOMapper {

    public TppDTO map(Tpp tpp){
        return TppDTO.builder()
                .state(tpp.getState())
                .messageUrl(tpp.getMessageUrl())
                .authenticationUrl(tpp.getAuthenticationUrl())
                .authenticationType(AuthenticationType.OAUTH2)
                .tppId(tpp.getTppId())
                .businessName(tpp.getBusinessName())
                .contact(tpp.getContact())
                .entityId(tpp.getEntityId())
                .build();
    }
}
