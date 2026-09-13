package br.gov.quixada.esporte.competicao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompeticaoRepository extends JpaRepository<Competicao, Long> {

    @Override
    @EntityGraph(attributePaths = "modalidade")
    Page<Competicao> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "modalidade")
    Page<Competicao> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "modalidade")
    Optional<Competicao> findById(Long id);
}
