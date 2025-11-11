package it.gov.pagopa.tpp.enums;

import lombok.Getter;

/**
 * Enumeration defining the authentication mechanisms for TPP.
 */
@Getter
public enum AuthenticationType {
    OAUTH2("OAUTH2");

    private final String type;

    AuthenticationType(String type) { this.type = type; }
}
