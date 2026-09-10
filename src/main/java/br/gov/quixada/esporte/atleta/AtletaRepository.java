package br.gov.quixada.esporte.atleta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtletaRepository extends JpaRepository<Atleta, Long> {

    boolean existsByCpf(String cpf);

    Optional<Atleta> findByCpf(String cpf);

    List<Atleta> findByNomeCompletoContainingIgnoreCase(String name);
}
