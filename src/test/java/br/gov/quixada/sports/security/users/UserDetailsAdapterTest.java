package br.gov.quixada.sports.security.users;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

@DisplayName("UserDetailsAdapter")
class UserDetailsAdapterTest {

    @Test
    @DisplayName("should use the email as username and delegate the password")
    void shouldUseEmailAsUsernameAndDelegatePassword() {
        var user = new User("john", "john@example.com", "hash", UserRole.USER);

        var adapter = new UserDetailsAdapter(user);

        assertThat(adapter.getUsername()).isEqualTo("john@example.com");
        assertThat(adapter.getPassword()).isEqualTo("hash");
        assertThat(adapter.user()).isSameAs(user);
    }

    @Test
    @DisplayName("should grant only ROLE_USER to a regular user")
    void shouldGrantRoleUser() {
        var adapter = new UserDetailsAdapter(
                new User("john", "john@example.com", "hash", UserRole.USER));

        assertThat(adapter.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("should grant ROLE_ADMIN and ROLE_USER to an admin")
    void shouldGrantAdminRoles() {
        var adapter = new UserDetailsAdapter(
                new User("admin", "admin@example.com", "hash", UserRole.ADMIN));

        assertThat(adapter.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("should grant ROLE_SUPERADMIN, ROLE_ADMIN and ROLE_USER to a superadmin")
    void shouldGrantSuperadminRoles() {
        var adapter = new UserDetailsAdapter(
                new User("super", "super@example.com", "hash", UserRole.SUPERADMIN));

        assertThat(adapter.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SUPERADMIN", "ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("should keep the account enabled and non-expiring")
    void shouldKeepAccountEnabled() {
        var adapter = new UserDetailsAdapter(
                new User("john", "john@example.com", "hash", UserRole.USER));

        assertThat(List.of(
                adapter.isAccountNonExpired(),
                adapter.isAccountNonLocked(),
                adapter.isCredentialsNonExpired(),
                adapter.isEnabled())).containsOnly(true);
    }

    @Test
    @DisplayName("should disable an inactive user")
    void shouldDisableInactiveUser() {
        var user = new User("john", "john@example.com", "hash", UserRole.USER);
        user.setStatus(UserStatus.INACTIVE);

        assertThat(new UserDetailsAdapter(user).isEnabled()).isFalse();
    }
}
