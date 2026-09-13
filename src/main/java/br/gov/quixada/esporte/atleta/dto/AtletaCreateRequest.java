package br.gov.quixada.esporte.atleta.dto;

import br.gov.quixada.esporte.extras.EnderecoRequest;
import br.gov.quixada.esporte.extras.Sexo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;


public record AtletaCreateRequest(
        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max=150)
        String nomeCompleto,
        @NotBlank(message = "Cpf é obrigatório")
        @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11}", message = "CPF deve estar no formato 000.000.000-00 ou 00000000000")
        //@CPF(message = "Cpf inválido") comentado para testes fake, posteriormente alterar.
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
        @Pattern(regexp = "\\d{10,12}", message = "Telefone deve conter apenas números e ter entre 10 e 12 dígitos")
        String telefone
) {}
