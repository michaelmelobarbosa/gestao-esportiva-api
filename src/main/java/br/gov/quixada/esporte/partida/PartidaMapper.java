package br.gov.quixada.esporte.partida;

import br.gov.quixada.esporte.partida.dto.PartidaResponse;
import br.gov.quixada.esporte.partida.dto.PartidaResumoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PartidaMapper {

    @Mapping(target = "competicaoId", source = "inscricaoMandante.competicao.id")
    @Mapping(target = "competicaoNome", source = "inscricaoMandante.competicao.nome")
    @Mapping(target = "categoriaId", source = "inscricaoMandante.categoria.id")
    @Mapping(target = "categoriaNome", source = "inscricaoMandante.categoria.nome")
    @Mapping(target = "inscricaoMandanteId", source = "inscricaoMandante.id")
    @Mapping(target = "mandanteEquipeId", source = "inscricaoMandante.equipe.id")
    @Mapping(target = "mandanteEquipeNome", source = "inscricaoMandante.equipe.nome")
    @Mapping(target = "inscricaoVisitanteId", source = "inscricaoVisitante.id")
    @Mapping(target = "visitanteEquipeId", source = "inscricaoVisitante.equipe.id")
    @Mapping(target = "visitanteEquipeNome", source = "inscricaoVisitante.equipe.nome")
    PartidaResponse toResponse(Partida partida);

    @Mapping(target = "competicaoId", source = "inscricaoMandante.competicao.id")
    @Mapping(target = "categoriaId", source = "inscricaoMandante.categoria.id")
    @Mapping(target = "mandanteEquipeId", source = "inscricaoMandante.equipe.id")
    @Mapping(target = "mandanteEquipeNome", source = "inscricaoMandante.equipe.nome")
    @Mapping(target = "visitanteEquipeId", source = "inscricaoVisitante.equipe.id")
    @Mapping(target = "visitanteEquipeNome", source = "inscricaoVisitante.equipe.nome")
    PartidaResumoResponse toResumo(Partida partida);
}
