package br.gov.quixada.esporte.inscricao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InscricaoEquipeRepository extends JpaRepository<InscricaoEquipe, Long> {

    @Override
    @EntityGraph(attributePaths = {"competicao", "categoria", "equipe"})
    Page<InscricaoEquipe> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"competicao", "categoria", "equipe"})
    Page<InscricaoEquipe> findByCompeticaoId(Long competicaoId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"competicao", "categoria", "equipe"})
    Optional<InscricaoEquipe> findById(Long id);
}
