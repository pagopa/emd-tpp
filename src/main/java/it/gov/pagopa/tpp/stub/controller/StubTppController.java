package it.gov.pagopa.tpp.stub.controller;

import it.gov.pagopa.tpp.stub.dto.BaseMessageDTO;
import it.gov.pagopa.tpp.stub.dto.TokenDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/**
 * REST controller interface for TPP stub operations.
 * <p>
 * Base Path: {@code /emd}
 */
@RequestMapping("/emd")
public interface StubTppController {

    /**
     * Simulates TPP authentication process with form-based credentials.
     * 
     * @param formData the form-encoded authentication data containing credentials
     * @return a Mono containing ResponseEntity with TokenDTO containing the authentication token
     */
    @PostMapping("/stub/auth")
    Mono<ResponseEntity<TokenDTO>> auth(@Valid @RequestParam MultiValueMap<String, String> formData);

    /**
     * Simulates TPP message processing operations.
     * 
     * @param message the message data to be processed by the TPP stub
     * @return a Mono containing ResponseEntity with String response from message processing
     */
    @PostMapping("/stub/message")
    Mono<ResponseEntity<String>> message(@Valid @RequestBody BaseMessageDTO message);

}
