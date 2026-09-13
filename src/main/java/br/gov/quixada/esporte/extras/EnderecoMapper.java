package br.gov.quixada.esporte.extras;

import jakarta.validation.Valid;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnderecoMapper {

    Endereco toEndereco(@Valid EnderecoRequest request);

    EnderecoResponse toResponse(@Valid Endereco endereco);
}
