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


@RequestMapping("/emd")
public interface StubTppController {

    @PostMapping("/stub/auth")
    Mono<ResponseEntity<TokenDTO>> auth(@Valid @RequestParam MultiValueMap<String, String> formData);

    @PostMapping("/stub/message")
    Mono<ResponseEntity<String>> message(@Valid @RequestBody BaseMessageDTO message);

}
