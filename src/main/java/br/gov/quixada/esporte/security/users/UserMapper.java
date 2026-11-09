package br.gov.quixada.esporte.security.users;

import br.gov.quixada.esporte.security.users.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
