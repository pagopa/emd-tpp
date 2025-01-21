package it.gov.pagopa.tpp.model;


import it.gov.pagopa.tpp.enums.AuthenticationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;

@Document(collection = "tpp")
@Data
@NoArgsConstructor
@SuperBuilder
public class Tpp {

    private String id;
    private String tppId;
    private String entityId;
    private String idPsp;
    private String businessName;
    private String legalAddress;
    private String messageUrl;
    private String authenticationUrl;
    private AuthenticationType authenticationType;
    private Boolean state;
    private Contact contact;
    private TokenSection tokenSection;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
    private String paymentButton;
    private HashMap<String, String> agentDeepLinks;
}