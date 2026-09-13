package br.gov.quixada.esporte.equipe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Long> {

    @Override
    @EntityGraph(attributePaths = {"clube", "modalidade"})
    Page<Equipe> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"clube", "modalidade"})
    Page<Equipe> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"clube", "modalidade"})
    Optional<Equipe> findById(Long id);
}
