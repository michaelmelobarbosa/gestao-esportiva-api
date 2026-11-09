package br.gov.quixada.esporte.atleta;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtletaRepository extends JpaRepository<Atleta, Long> {

    Optional<Atleta> findByCpf(String cpf);

    Page<Atleta> findByNomeCompletoNormalizadoStartingWith(String nomeCompletoNormalizado, Pageable pageable);

    Page<Atleta> findByStatus(StatusAtleta status, Pageable pageable);

    Page<Atleta> findByNomeCompletoNormalizadoStartingWithAndStatus(String nomeCompletoNormalizado,
                                                                    StatusAtleta status, Pageable pageable);
}
