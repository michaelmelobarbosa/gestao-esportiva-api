package br.gov.quixada.esporte.users.auth;

import br.gov.quixada.esporte.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationRepository extends JpaRepository<User, Integer> {

    UserDetails findByUsername(String username);
}
