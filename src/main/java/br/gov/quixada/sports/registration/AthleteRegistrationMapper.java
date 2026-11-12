package br.gov.quixada.sports.registration;

import br.gov.quixada.sports.registration.dto.AthleteRegistrationCreateRequest;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationResponse;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationSummaryResponse;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AthleteRegistrationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "athlete", ignore = true)
    @Mapping(target = "teamRegistration", ignore = true)
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "canceledAt", ignore = true)
    AthleteRegistration toEntity(@Valid AthleteRegistrationCreateRequest request);

    @Mapping(target = "teamRegistrationId", source = "teamRegistration.id")
    @Mapping(target = "athleteId", source = "athlete.id")
    @Mapping(target = "athleteName", source = "athlete.fullName")
    AthleteRegistrationResponse toResponse(AthleteRegistration athleteRegistration);

    @Mapping(target = "teamRegistrationId", source = "teamRegistration.id")
    @Mapping(target = "athleteId", source = "athlete.id")
    @Mapping(target = "athleteName", source = "athlete.fullName")
    AthleteRegistrationSummaryResponse toSummary(AthleteRegistration athleteRegistration);

    List<AthleteRegistrationSummaryResponse> toSummaryList(List<AthleteRegistration> registrations);
}
