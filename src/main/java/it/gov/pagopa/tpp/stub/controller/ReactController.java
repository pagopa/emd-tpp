package it.gov.pagopa.tpp.stub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller for handling React application routing.
 * <p>
 * This controller provides client-side routing support for React applications by
 * forwarding all non-API requests to the main index.html file.
 */
@Controller
public class ReactController {

    /**
     * Redirects all non-static resource requests to the React application's index.html.
     * 
     * @param path the requested path that will be handled
     * @return a forward directive to index.html for React application handling
     */
    @GetMapping("/{path:[^\\.]*}")
    public String redirect(@PathVariable String path) {
        return "forward:/index.html";
    }
}
