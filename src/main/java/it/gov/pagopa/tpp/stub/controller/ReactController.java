package it.gov.pagopa.tpp.stub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ReactController {
    @GetMapping("/{path:[^\\.]*}")
    public String redirect(@PathVariable String path) {
        return "forward:/index.html";
    }
}
