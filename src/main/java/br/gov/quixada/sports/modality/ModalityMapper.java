package br.gov.quixada.sports.modality;

import br.gov.quixada.sports.modality.dto.ModalityCreateRequest;
import br.gov.quixada.sports.modality.dto.ModalityResponse;
import br.gov.quixada.sports.modality.dto.ModalitySummaryResponse;
import br.gov.quixada.sports.modality.dto.ModalityUpdateRequest;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ModalityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Modality toEntity(@Valid ModalityCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Modality toEntity(@Valid ModalityUpdateRequest request);

    ModalityResponse toResponse(Modality modality);

    ModalitySummaryResponse toSummary(Modality modality);

    List<ModalitySummaryResponse> toSummaryList(List<Modality> modalities);
}
