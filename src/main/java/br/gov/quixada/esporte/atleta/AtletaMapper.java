package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.atleta.dto.AtletaResponse;
import br.gov.quixada.esporte.atleta.dto.AtletaCreateRequest;
import br.gov.quixada.esporte.atleta.dto.AtletaUpdateRequest;
import br.gov.quixada.esporte.atleta.dto.AtletaResumoResponse;
import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.EnderecoRequest;
import br.gov.quixada.esporte.extras.EnderecoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AtletaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    Atleta toEntity(AtletaCreateRequest postRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cpf", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    Atleta updateEntity(AtletaUpdateRequest putRequest);

    @Mapping(target = "idade", expression = "java(calcularIdade(atleta.getDataNascimento()))")
    AtletaResponse toGetResponse(Atleta atleta);

    List<AtletaResumoResponse> toResumoList(List<Atleta> atletas);

    Endereco toEndereco(EnderecoRequest request);

    EnderecoResponse toEnderecoResponse(Endereco endereco);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cpf", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    public void updateEntityFromEntity(Atleta atletaAtualizado, @MappingTarget Atleta atletaExistente);

    default Integer calcularIdade(LocalDate data) {
        return data == null ? null : java.time.Period.between(data, java
                .time.LocalDate.now()).getYears();
    }
}


