package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.inscricao.dto.InscricaoEquipeCreateRequest;
import br.gov.quixada.esporte.inscricao.dto.InscricaoEquipeResponse;
import br.gov.quixada.esporte.inscricao.dto.InscricaoEquipeResumoResponse;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InscricaoEquipeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "competicao", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "equipe", ignore = true)
    @Mapping(target = "dataInscricao", ignore = true)
    @Mapping(target = "status", ignore = true)
    InscricaoEquipe toEntity(@Valid InscricaoEquipeCreateRequest request);

    @Mapping(target = "competicaoId", source = "competicao.id")
    @Mapping(target = "competicaoNome", source = "competicao.nome")
    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoriaNome", source = "categoria.nome")
    @Mapping(target = "equipeId", source = "equipe.id")
    @Mapping(target = "equipeNome", source = "equipe.nome")
    InscricaoEquipeResponse toResponse(InscricaoEquipe inscricaoEquipe);

    @Mapping(target = "competicaoId", source = "competicao.id")
    @Mapping(target = "competicaoNome", source = "competicao.nome")
    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoriaNome", source = "categoria.nome")
    @Mapping(target = "equipeId", source = "equipe.id")
    @Mapping(target = "equipeNome", source = "equipe.nome")
    InscricaoEquipeResumoResponse toResumo(InscricaoEquipe inscricaoEquipe);

    List<InscricaoEquipeResumoResponse> toResumoList(List<InscricaoEquipe> inscricoes);
}
