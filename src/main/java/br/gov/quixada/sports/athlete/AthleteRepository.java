package br.gov.quixada.sports.athlete;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AthleteRepository extends JpaRepository<Athlete, Long> {

    Optional<Athlete> findByCpf(String cpf);

    Page<Athlete> findByNormalizedFullNameStartingWith(String normalizedFullName, Pageable pageable);

    Page<Athlete> findByStatus(AthleteStatus status, Pageable pageable);

    Page<Athlete> findByNormalizedFullNameStartingWithAndStatus(String normalizedFullName,
                                                                    AthleteStatus status, Pageable pageable);
}
