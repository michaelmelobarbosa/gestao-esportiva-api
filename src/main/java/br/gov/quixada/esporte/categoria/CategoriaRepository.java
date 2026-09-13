package br.gov.quixada.esporte.categoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Override
    @EntityGraph(attributePaths = "competicao")
    Page<Categoria> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "competicao")
    Page<Categoria> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "competicao")
    Optional<Categoria> findById(Long id);
}
