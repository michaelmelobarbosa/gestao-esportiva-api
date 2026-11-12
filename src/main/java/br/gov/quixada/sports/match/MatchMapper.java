package br.gov.quixada.sports.match;

import br.gov.quixada.sports.match.dto.MatchResponse;
import br.gov.quixada.sports.match.dto.MatchSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    @Mapping(target = "competitionId", source = "homeRegistration.competition.id")
    @Mapping(target = "competitionName", source = "homeRegistration.competition.name")
    @Mapping(target = "categoryId", source = "homeRegistration.category.id")
    @Mapping(target = "categoryName", source = "homeRegistration.category.name")
    @Mapping(target = "homeRegistrationId", source = "homeRegistration.id")
    @Mapping(target = "homeTeamId", source = "homeRegistration.team.id")
    @Mapping(target = "homeTeamName", source = "homeRegistration.team.name")
    @Mapping(target = "awayRegistrationId", source = "awayRegistration.id")
    @Mapping(target = "awayTeamId", source = "awayRegistration.team.id")
    @Mapping(target = "awayTeamName", source = "awayRegistration.team.name")
    MatchResponse toResponse(Match match);

    @Mapping(target = "competitionId", source = "homeRegistration.competition.id")
    @Mapping(target = "categoryId", source = "homeRegistration.category.id")
    @Mapping(target = "homeTeamId", source = "homeRegistration.team.id")
    @Mapping(target = "homeTeamName", source = "homeRegistration.team.name")
    @Mapping(target = "awayTeamId", source = "awayRegistration.team.id")
    @Mapping(target = "awayTeamName", source = "awayRegistration.team.name")
    MatchSummaryResponse toSummary(Match match);
}
