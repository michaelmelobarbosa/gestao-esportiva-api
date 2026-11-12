package br.gov.quixada.sports.shared;

public record AddressResponse(
        String street,
        String number,
        String neighborhood,
        String city,
        String postalCode
) {
}
