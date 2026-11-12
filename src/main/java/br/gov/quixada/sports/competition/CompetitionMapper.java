package br.gov.quixada.sports.competition;

import br.gov.quixada.sports.competition.dto.CompetitionCreateRequest;
import br.gov.quixada.sports.competition.dto.CompetitionResponse;
import br.gov.quixada.sports.competition.dto.CompetitionSummaryResponse;
import br.gov.quixada.sports.competition.dto.CompetitionUpdateRequest;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompetitionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modality", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "canceledAt", ignore = true)
    Competition toEntity(@Valid CompetitionCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modality", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "canceledAt", ignore = true)
    Competition toEntity(@Valid CompetitionUpdateRequest request);

    @Mapping(target = "modalityId", source = "modality.id")
    @Mapping(target = "modalityName", source = "modality.name")
    CompetitionResponse toResponse(Competition competition);

    @Mapping(target = "modalityId", source = "modality.id")
    @Mapping(target = "modalityName", source = "modality.name")
    CompetitionSummaryResponse toSummary(Competition competition);

    List<CompetitionSummaryResponse> toSummaryList(List<Competition> competitions);
}
