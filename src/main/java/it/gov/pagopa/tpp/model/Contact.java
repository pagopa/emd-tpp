package it.gov.pagopa.tpp.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Contact {
    private String name;

    @Pattern(regexp = "^\\d{9,10}$", message = "Number must be between 9 and 10 digits")
    private String number;

    @Email(message = "Email must be valid")
    private String email;
}
