package br.gov.quixada.esporte.competicao;

import br.gov.quixada.esporte.competicao.dto.CompeticaoCreateRequest;
import br.gov.quixada.esporte.competicao.dto.CompeticaoResponse;
import br.gov.quixada.esporte.competicao.dto.CompeticaoResumoResponse;
import br.gov.quixada.esporte.competicao.dto.CompeticaoUpdateRequest;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompeticaoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modalidade", ignore = true)
    @Mapping(target = "status", ignore = true)
    Competicao toEntity(@Valid CompeticaoCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modalidade", ignore = true)
    @Mapping(target = "status", ignore = true)
    Competicao toEntity(@Valid CompeticaoUpdateRequest request);

    @Mapping(target = "modalidadeId", source = "modalidade.id")
    @Mapping(target = "modalidadeNome", source = "modalidade.nome")
    CompeticaoResponse toResponse(Competicao competicao);

    @Mapping(target = "modalidadeId", source = "modalidade.id")
    @Mapping(target = "modalidadeNome", source = "modalidade.nome")
    CompeticaoResumoResponse toResumo(Competicao competicao);

    List<CompeticaoResumoResponse> toResumoList(List<Competicao> competicoes);
}
