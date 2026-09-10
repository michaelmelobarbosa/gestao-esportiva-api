package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.extras.EnderecoRequest;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;


public record AtletaCreateRequest(
        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max=150)
        String nomeCompleto,
        @NotBlank(message = "Cpf é obrigatório")
        @CPF(message = "Cpf inválido")
        @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11}", message = "CPF deve estar no formato 000.000.000-00 ou 00000000000")
        String cpf,
        @Past(message = "A data de nascimento deve ser no passado")
        @NotNull(message = "Data de nascimento é obrigatório")
        LocalDate dataNascimento,
        @NotNull(message = "Endereço é obrigatório")
        @Valid
        EnderecoRequest endereco,
        @NotNull(message = "Sexo é obrigatório")
        Sexo sexo,
        @NotBlank(message = "Telefone é obrigatório")
        @Size(max=20)
        String telefone
) {}
