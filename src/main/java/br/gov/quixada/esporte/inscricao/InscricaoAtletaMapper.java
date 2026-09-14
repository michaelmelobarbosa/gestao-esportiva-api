package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.inscricao.dto.InscricaoAtletaCreateRequest;
import br.gov.quixada.esporte.inscricao.dto.InscricaoAtletaResponse;
import br.gov.quixada.esporte.inscricao.dto.InscricaoAtletaResumoResponse;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InscricaoAtletaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "atleta", ignore = true)
    @Mapping(target = "inscricaoEquipe", ignore = true)
    @Mapping(target = "dataInscricao", ignore = true)
    @Mapping(target = "status", ignore = true)
    InscricaoAtleta toEntity(@Valid InscricaoAtletaCreateRequest request);

    @Mapping(target = "inscricaoEquipeId", source = "inscricaoEquipe.id")
    @Mapping(target = "atletaId", source = "atleta.id")
    @Mapping(target = "atletaNome", source = "atleta.nomeCompleto")
    InscricaoAtletaResponse toResponse(InscricaoAtleta inscricaoAtleta);

    @Mapping(target = "inscricaoEquipeId", source = "inscricaoEquipe.id")
    @Mapping(target = "atletaId", source = "atleta.id")
    @Mapping(target = "atletaNome", source = "atleta.nomeCompleto")
    InscricaoAtletaResumoResponse toResumo(InscricaoAtleta inscricaoAtleta);

    List<InscricaoAtletaResumoResponse> toResumoList(List<InscricaoAtleta> inscricoes);
}
