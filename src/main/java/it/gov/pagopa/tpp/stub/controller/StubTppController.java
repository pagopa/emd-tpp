package it.gov.pagopa.tpp.stub.controller;

import it.gov.pagopa.tpp.stub.dto.BaseMessageDTO;
import it.gov.pagopa.tpp.stub.dto.TokenDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RequestMapping("/stub/emd/tpp")
public interface StubTppController {

    @PostMapping("/stub/auth")
    Mono<ResponseEntity<TokenDTO>> auth(@Valid @RequestBody MultiValueMap<String, String> formData);

    @PostMapping("/stub/message")
    Mono<ResponseEntity<String>> message(@Valid @RequestBody BaseMessageDTO message);

}
