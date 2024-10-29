package it.gov.pagopa.tpp.model;


import it.gov.pagopa.tpp.enums.AuthenticationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "tpp")
@Data
@SuperBuilder
@NoArgsConstructor
public class Tpp {

    private String id;
    private String tppId;
    private String entityId;
    private String idPsp;
    private String businessName;
    private String messageUrl;
    private String authenticationUrl;
    private AuthenticationType authenticationType;
    private Boolean state;
    private Contact contact;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
}