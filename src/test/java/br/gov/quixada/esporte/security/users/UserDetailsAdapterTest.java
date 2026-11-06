package br.gov.quixada.esporte.security.users;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

@DisplayName("UserDetailsAdapter")
class UserDetailsAdapterTest {

    @Test
    @DisplayName("deve usar o email como username e delegar a senha")
    void deveUsarEmailComoSenhaEDelegarSenha() {
        var user = new User("fulano", "fulano@example.com", "hash", UserRole.USER);

        var adapter = new UserDetailsAdapter(user);

        assertThat(adapter.getUsername()).isEqualTo("fulano@example.com");
        assertThat(adapter.getPassword()).isEqualTo("hash");
        assertThat(adapter.user()).isSameAs(user);
    }

    @Test
    @DisplayName("deve conceder apenas ROLE_USER para usuário comum")
    void deveConcederRoleUser() {
        var adapter = new UserDetailsAdapter(
                new User("fulano", "fulano@example.com", "hash", UserRole.USER));

        assertThat(adapter.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("deve conceder ROLE_ADMIN e ROLE_USER para administrador")
    void deveConcederRolesDeAdmin() {
        var adapter = new UserDetailsAdapter(
                new User("admin", "admin@example.com", "hash", UserRole.ADMIN));

        assertThat(adapter.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("deve conceder ROLE_SUPERADMIN, ROLE_ADMIN e ROLE_USER para superadministrador")
    void deveConcederRolesDeSuperadmin() {
        var adapter = new UserDetailsAdapter(
                new User("super", "super@example.com", "hash", UserRole.SUPERADMIN));

        assertThat(adapter.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SUPERADMIN", "ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("deve manter a conta habilitada e sem expiração")
    void deveManterContaHabilitada() {
        var adapter = new UserDetailsAdapter(
                new User("fulano", "fulano@example.com", "hash", UserRole.USER));

        assertThat(List.of(
                adapter.isAccountNonExpired(),
                adapter.isAccountNonLocked(),
                adapter.isCredentialsNonExpired(),
                adapter.isEnabled())).containsOnly(true);
    }

    @Test
    @DisplayName("deve desabilitar usuário inativo")
    void deveDesabilitarUsuarioInativo() {
        var user = new User("fulano", "fulano@example.com", "hash", UserRole.USER);
        user.setStatus(UserStatus.INATIVO);

        assertThat(new UserDetailsAdapter(user).isEnabled()).isFalse();
    }
}
