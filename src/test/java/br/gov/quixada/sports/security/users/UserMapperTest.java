package br.gov.quixada.sports.security.users;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserMapper")
class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserMapperImpl();
    }

    @Test
    @DisplayName("toResponse should map public user fields without exposing password")
    void toResponseShouldMapFields() {
        var user = new User(1L, "admin", "admin@quixada.gov.br", "bcrypt-hash", UserRole.ADMIN, UserStatus.ACTIVE);

        var response = mapper.toResponse(user);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.email()).isEqualTo("admin@quixada.gov.br");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("toResponse should return null when user is null")
    void toResponseWithNullShouldReturnNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
