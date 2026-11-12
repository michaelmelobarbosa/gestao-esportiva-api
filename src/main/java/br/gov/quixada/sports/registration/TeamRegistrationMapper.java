package br.gov.quixada.sports.registration;

import br.gov.quixada.sports.registration.dto.TeamRegistrationCreateRequest;
import br.gov.quixada.sports.registration.dto.TeamRegistrationResponse;
import br.gov.quixada.sports.registration.dto.TeamRegistrationSummaryResponse;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TeamRegistrationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "competition", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "canceledAt", ignore = true)
    TeamRegistration toEntity(@Valid TeamRegistrationCreateRequest request);

    @Mapping(target = "competitionId", source = "competition.id")
    @Mapping(target = "competitionName", source = "competition.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    TeamRegistrationResponse toResponse(TeamRegistration teamRegistration);

    @Mapping(target = "competitionId", source = "competition.id")
    @Mapping(target = "competitionName", source = "competition.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    TeamRegistrationSummaryResponse toSummary(TeamRegistration teamRegistration);

    List<TeamRegistrationSummaryResponse> toSummaryList(List<TeamRegistration> registrations);
}
