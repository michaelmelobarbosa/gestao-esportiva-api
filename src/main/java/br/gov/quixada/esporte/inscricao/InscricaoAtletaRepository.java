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
public interface InscricaoAtletaRepository extends JpaRepository<InscricaoAtleta, Long> {

    @Override
    @EntityGraph(attributePaths = {"atleta", "inscricaoEquipe"})
    Page<InscricaoAtleta> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"atleta", "inscricaoEquipe"})
    Page<InscricaoAtleta> findByInscricaoEquipeId(Long inscricaoEquipeId, Pageable pageable);

    @EntityGraph(attributePaths = {"atleta", "inscricaoEquipe"})
    Page<InscricaoAtleta> findByStatus(StatusInscricao status, Pageable pageable);

    @EntityGraph(attributePaths = {"atleta", "inscricaoEquipe"})
    Page<InscricaoAtleta> findByInscricaoEquipeIdAndStatus(Long inscricaoEquipeId, StatusInscricao status,
                                                           Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"atleta", "inscricaoEquipe"})
    Optional<InscricaoAtleta> findById(Long id);

    List<InscricaoAtleta> findByInscricaoEquipeId(Long inscricaoEquipeId);

    boolean existsByAtletaIdAndStatusIn(Long atletaId, Collection<StatusInscricao> statuses);

    boolean existsByInscricaoEquipeIdAndAtletaIdAndStatusIn(Long inscricaoEquipeId, Long atletaId,
                                                            Collection<StatusInscricao> statuses);

    long countByInscricaoEquipeIdAndStatusIn(Long inscricaoEquipeId, Collection<StatusInscricao> statuses);

    boolean existsByInscricaoEquipeIdAndNumeroCamisaAndStatusIn(Long inscricaoEquipeId, String numeroCamisa,
                                                                Collection<StatusInscricao> statuses);

    boolean existsByInscricaoEquipeCompeticaoIdAndAtletaIdAndStatusIn(Long competicaoId, Long atletaId,
                                                                      Collection<StatusInscricao> statuses);

    boolean existsByInscricaoEquipeIdAndAtletaIdAndIdNotAndStatusIn(Long inscricaoEquipeId, Long atletaId, Long id,
                                                                   Collection<StatusInscricao> statuses);

    boolean existsByInscricaoEquipeIdAndNumeroCamisaAndIdNotAndStatusIn(Long inscricaoEquipeId, String numeroCamisa,
                                                                       Long id, Collection<StatusInscricao> statuses);

    boolean existsByInscricaoEquipeCompeticaoIdAndAtletaIdAndIdNotAndStatusIn(Long competicaoId, Long atletaId,
                                                                             Long id,
                                                                             Collection<StatusInscricao> statuses);
}
