package br.gov.quixada.sports.club;

import br.gov.quixada.sports.club.dto.ClubCreateRequest;
import br.gov.quixada.sports.club.dto.ClubResponse;
import br.gov.quixada.sports.club.dto.ClubSummaryResponse;
import br.gov.quixada.sports.club.dto.ClubUpdateRequest;
import br.gov.quixada.sports.shared.AddressMapper;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = AddressMapper.class)
public interface ClubMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Club toEntity(@Valid ClubCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    Club toEntity(@Valid ClubUpdateRequest request);

    ClubResponse toResponse(Club club);

    ClubSummaryResponse toSummary(Club club);

    List<ClubSummaryResponse> toSummaryList(List<Club> clubs);
}
