package br.gov.quixada.sports.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@NoArgsConstructor 
@AllArgsConstructor 
@Getter 
@Setter 
public class Address {

    @Column(nullable = false, length = 150)
    @NotBlank
    @Size(max = 150)
    private String street;

    @Column(nullable = false, length = 10)
    @NotBlank
    @Size(max = 10)
    private String number;

    @Column(nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String neighborhood;

    @Column(nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String city;

    @Column(nullable = false, length = 10)
    @NotBlank
    @Pattern(regexp="\\d{5}-\\d{3}", message = "Postal code must follow the pattern 00000-000")
    private String postalCode;
}
