package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;


public record AtletaPostRequest(
        @NotBlank(message = "Nome completo é obrigatório")
        String nomeCompleto,
        @NotBlank(message = "Cpf é obrigatório")
        @CPF(message = "Cpf inválido")
        @Size(max = 14)
        String cpf,
        @NotBlank(message = "Data de nascimento é obrigatório")
        LocalDate dataNascimento,
        @NotBlank(message = "Endereço é obrigatório")
        @Valid
        Endereco endereco,
        @NotNull
        Sexo sexo,
        @NotBlank(message = "Telefone é obrigatório")
        String telefone
) {}
