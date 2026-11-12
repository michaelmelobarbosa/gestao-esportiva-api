package br.gov.quixada.sports.club.dto;

import br.gov.quixada.sports.club.ClubStatus;
import br.gov.quixada.sports.shared.AddressResponse;

public record ClubResponse(
        Long id,
        String name,
        String manager,
        String phone,
        AddressResponse address,
        ClubStatus status
) {
}
