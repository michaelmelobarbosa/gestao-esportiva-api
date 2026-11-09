package br.gov.quixada.esporte.inscricao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InscricaoEquipeRepository extends JpaRepository<InscricaoEquipe, Long> {

    @Override
    @EntityGraph(attributePaths = {"competicao", "categoria", "equipe"})
    Page<InscricaoEquipe> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"competicao", "categoria", "equipe"})
    Page<InscricaoEquipe> findByCompeticaoId(Long competicaoId, Pageable pageable);

    List<InscricaoEquipe> findByCompeticaoId(Long competicaoId);

    boolean existsByCompeticaoIdAndEquipeIdAndStatusIn(Long competicaoId, Long equipeId,
                                                       Collection<StatusInscricao> statuses);

    boolean existsByCompeticaoIdAndEquipeIdAndStatusInAndIdNot(Long competicaoId, Long equipeId,
                                                               Collection<StatusInscricao> statuses, Long id);

    @EntityGraph(attributePaths = {"competicao", "categoria", "equipe"})
    List<InscricaoEquipe> findByCompeticaoIdAndCategoriaIdAndStatus(Long competicaoId, Long categoriaId,
                                                                    StatusInscricao status);

    boolean existsByEquipeIdAndStatusIn(Long equipeId, Collection<StatusInscricao> statuses);

    boolean existsByCategoriaIdAndStatusIn(Long categoriaId, Collection<StatusInscricao> statuses);

    @EntityGraph(attributePaths = {"competicao", "categoria", "equipe"})
    Page<InscricaoEquipe> findByStatus(StatusInscricao status, Pageable pageable);

    @EntityGraph(attributePaths = {"competicao", "categoria", "equipe"})
    Page<InscricaoEquipe> findByCompeticaoIdAndStatus(Long competicaoId, StatusInscricao status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"competicao", "categoria", "equipe"})
    Optional<InscricaoEquipe> findById(Long id);
}
