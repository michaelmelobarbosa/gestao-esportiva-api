package br.gov.quixada.sports.athlete;

import br.gov.quixada.sports.athlete.dto.AthleteCreateRequest;
import br.gov.quixada.sports.athlete.dto.AthleteResponse;
import br.gov.quixada.sports.athlete.dto.AthleteSummaryResponse;
import br.gov.quixada.sports.athlete.dto.AthleteUpdateRequest;
import br.gov.quixada.sports.shared.AddressMapper;
import jakarta.validation.Valid;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

@Mapper(componentModel = "spring", uses = AddressMapper.class)
public interface AthleteMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "normalizedFullName", ignore = true)
    Athlete toEntity(@Valid AthleteCreateRequest postRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "cpf", ignore = true)
    @Mapping(target = "normalizedFullName", ignore = true)
    Athlete toEntity(@Valid AthleteUpdateRequest request);

    @Mapping(target = "age", expression = "java(calculateAge(athlete.getBirthDate(), clock))")
    AthleteResponse toGetResponse(Athlete athlete, @Context Clock clock);

    @Mapping(target = "age", expression = "java(calculateAge(athlete.getBirthDate(), clock))")
    AthleteSummaryResponse toSummary(Athlete athlete, @Context Clock clock);

    default Integer calculateAge(LocalDate date, Clock clock) {
        return date == null ? null : Period.between(date, LocalDate.now(clock)).getYears();
    }
}
