package br.gov.quixada.sports.security.users;

import br.gov.quixada.sports.security.users.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
