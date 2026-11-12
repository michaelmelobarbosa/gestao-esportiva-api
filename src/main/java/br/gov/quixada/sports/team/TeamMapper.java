package br.gov.quixada.sports.team;

import br.gov.quixada.sports.team.dto.TeamCreateRequest;
import br.gov.quixada.sports.team.dto.TeamResponse;
import br.gov.quixada.sports.team.dto.TeamSummaryResponse;
import br.gov.quixada.sports.team.dto.TeamUpdateRequest;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "modality", ignore = true)
    @Mapping(target = "status", ignore = true)
    Team toEntity(@Valid TeamCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "modality", ignore = true)
    @Mapping(target = "status", ignore = true)
    Team toEntity(@Valid TeamUpdateRequest request);

    @Mapping(target = "clubId", source = "club.id")
    @Mapping(target = "clubName", source = "club.name")
    @Mapping(target = "modalityId", source = "modality.id")
    @Mapping(target = "modalityName", source = "modality.name")
    TeamResponse toResponse(Team team);

    @Mapping(target = "clubId", source = "club.id")
    @Mapping(target = "clubName", source = "club.name")
    @Mapping(target = "modalityId", source = "modality.id")
    @Mapping(target = "modalityName", source = "modality.name")
    TeamSummaryResponse toSummary(Team team);

    List<TeamSummaryResponse> toSummaryList(List<Team> teams);
}
