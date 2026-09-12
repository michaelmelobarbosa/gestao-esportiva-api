package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.atleta.dto.AtletaCreateRequest;
import br.gov.quixada.esporte.atleta.dto.AtletaResponse;
import br.gov.quixada.esporte.atleta.dto.AtletaResumoResponse;
import br.gov.quixada.esporte.atleta.dto.AtletaUpdateRequest;
import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.EnderecoRequest;
import br.gov.quixada.esporte.extras.EnderecoResponse;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface AtletaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "status", ignore = true)
    Atleta toEntity(@Valid AtletaCreateRequest postRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "cpf", ignore = true)
    Atleta toEntity(@Valid AtletaUpdateRequest request);

    @Mapping(target = "idade", expression = "java(calcularIdade(atleta.getDataNascimento()))")
    AtletaResponse toGetResponse(Atleta atleta);

    @Mapping(target = "idade", expression = "java(calcularIdade(atleta.getDataNascimento()))")
    AtletaResumoResponse toResumo(Atleta atleta);

    Endereco toEndereco(@Valid EnderecoRequest request);

    EnderecoResponse toEnderecoResponse(@Valid Endereco endereco);

    default Integer calcularIdade(LocalDate data) {
        return data == null ? null : java.time.Period.between(data, java.time.LocalDate.now()).getYears();
    }


}
