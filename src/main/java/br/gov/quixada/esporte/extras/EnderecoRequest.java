package br.gov.quixada.esporte.extras;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank(message = "Logradouro é obrigatório")
        @Size(max = 150)
        String logradouro,
        @NotBlank(message = "Inserir número ou S/N")
        @Size(max = 10)
        String numero,
        @NotBlank(message = "Bairro é obrigatório")
        @Size(max=50)
        String bairro,
        @NotBlank(message = "Cidade é obrigatório")
        @Size(max=50)
        String cidade,
        @NotBlank(message = "Cep é obrigatório")
        @Pattern(regexp="\\d{5}-\\d{3}", message = "O padrão do cep deve ser xxxxx-xxx")
        String cep
) {
}
