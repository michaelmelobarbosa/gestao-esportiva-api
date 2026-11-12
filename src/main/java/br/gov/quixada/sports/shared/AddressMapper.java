package br.gov.quixada.sports.shared;

import jakarta.validation.Valid;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    Address toAddress(@Valid AddressRequest request);

    AddressResponse toResponse(@Valid Address address);
}
