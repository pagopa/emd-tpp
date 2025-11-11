package it.gov.pagopa.tpp.stub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing an authentication token response.
 */
@Data
@Getter
@NoArgsConstructor
public class TokenDTO {
    @JsonAlias("access_token")
    private String accessToken;
    @JsonAlias("token_type")
    private String tokenType;
    @JsonAlias("expires_in")
    private long expiresIn;
}
