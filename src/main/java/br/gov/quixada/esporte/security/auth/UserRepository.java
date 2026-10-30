package br.gov.quixada.esporte.security.auth;

import br.gov.quixada.esporte.security.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserRepository extends JpaRepository<User, Long> {

    UserDetails findByEmail(String email);

    boolean existsByEmail(String email);
}
