package br.gov.quixada.esporte.equipe;

import br.gov.quixada.esporte.equipe.dto.EquipeCreateRequest;
import br.gov.quixada.esporte.equipe.dto.EquipeResponse;
import br.gov.quixada.esporte.equipe.dto.EquipeResumoResponse;
import br.gov.quixada.esporte.equipe.dto.EquipeUpdateRequest;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EquipeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clube", ignore = true)
    @Mapping(target = "modalidade", ignore = true)
    @Mapping(target = "status", ignore = true)
    Equipe toEntity(@Valid EquipeCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clube", ignore = true)
    @Mapping(target = "modalidade", ignore = true)
    @Mapping(target = "status", ignore = true)
    Equipe toEntity(@Valid EquipeUpdateRequest request);

    @Mapping(target = "clubeId", source = "clube.id")
    @Mapping(target = "clubeNome", source = "clube.nome")
    @Mapping(target = "modalidadeId", source = "modalidade.id")
    @Mapping(target = "modalidadeNome", source = "modalidade.nome")
    EquipeResponse toResponse(Equipe equipe);

    @Mapping(target = "clubeId", source = "clube.id")
    @Mapping(target = "clubeNome", source = "clube.nome")
    @Mapping(target = "modalidadeId", source = "modalidade.id")
    @Mapping(target = "modalidadeNome", source = "modalidade.nome")
    EquipeResumoResponse toResumo(Equipe equipe);

    List<EquipeResumoResponse> toResumoList(List<Equipe> equipes);
}
