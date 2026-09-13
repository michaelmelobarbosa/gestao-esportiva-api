package br.gov.quixada.esporte.clube;

import br.gov.quixada.esporte.clube.dto.ClubeCreateRequest;
import br.gov.quixada.esporte.clube.dto.ClubeResponse;
import br.gov.quixada.esporte.clube.dto.ClubeResumoResponse;
import br.gov.quixada.esporte.clube.dto.ClubeUpdateRequest;
import br.gov.quixada.esporte.extras.EnderecoMapper;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = EnderecoMapper.class)
public interface ClubeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Clube toEntity(@Valid ClubeCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Clube toEntity(@Valid ClubeUpdateRequest request);

    ClubeResponse toResponse(Clube clube);

    ClubeResumoResponse toResumo(Clube clube);

    List<ClubeResumoResponse> toResumoList(List<Clube> clubes);
}
