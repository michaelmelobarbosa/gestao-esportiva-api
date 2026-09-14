package br.gov.quixada.esporte.modalidade;

import br.gov.quixada.esporte.modalidade.dto.ModalidadeCreateRequest;
import br.gov.quixada.esporte.modalidade.dto.ModalidadeResponse;
import br.gov.quixada.esporte.modalidade.dto.ModalidadeResumoResponse;
import br.gov.quixada.esporte.modalidade.dto.ModalidadeUpdateRequest;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ModalidadeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Modalidade toEntity(@Valid ModalidadeCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Modalidade toEntity(@Valid ModalidadeUpdateRequest request);

    ModalidadeResponse toResponse(Modalidade modalidade);

    ModalidadeResumoResponse toResumo(Modalidade modalidade);

    List<ModalidadeResumoResponse> toResumoList(List<Modalidade> modalidades);
}
