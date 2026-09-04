package br.gov.quixada.esporte.extras;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Embeddable
public class Endereco {

    @Column(nullable = false, length = 150)
    @NotBlank
    @Size(max = 150)
    private String logradouro;

    @Column(nullable = false, length = 10)
    @NotBlank
    @Size(max = 10)
    private String numero;

    @Column(nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String bairro;

    @Column(nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String cidade;

    @Column(nullable = false, length = 10)
    @NotBlank
    @Size(max = 10)
    @Pattern(regexp="\\d{5}-\\d{3}")
    private String cep;
}
