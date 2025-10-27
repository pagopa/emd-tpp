package it.gov.pagopa.tpp.dto;

import lombok.Data;

/**
 * Data Transfer Object representing the response from network connectivity tests.
 */
@Data
public class NetworkResponseDTO {
    private String code;
    private String message;
}
