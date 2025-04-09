package it.gov.pagopa.tpp.stub.controller;

import it.gov.pagopa.tpp.stub.dto.BaseMessageDTO;
import it.gov.pagopa.tpp.stub.dto.TokenDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class StubTppControllerImpl implements StubTppController {

    @Override
    public Mono<ResponseEntity<TokenDTO>> auth(MultiValueMap<String, String> formData){
        return Mono.just(ResponseEntity.ok(new TokenDTO()));
    }

    @Override
    public Mono<ResponseEntity<String>> message(BaseMessageDTO message){
        return Mono.just(ResponseEntity.ok(""));
    }

}
