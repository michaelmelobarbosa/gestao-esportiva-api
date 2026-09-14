package br.gov.quixada.esporte.inscricao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InscricaoAtletaRepository extends JpaRepository<InscricaoAtleta, Long> {

    @Override
    @EntityGraph(attributePaths = {"atleta", "inscricaoEquipe"})
    Page<InscricaoAtleta> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"atleta", "inscricaoEquipe"})
    Page<InscricaoAtleta> findByInscricaoEquipeId(Long inscricaoEquipeId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"atleta", "inscricaoEquipe"})
    Optional<InscricaoAtleta> findById(Long id);

    boolean existsByInscricaoEquipeIdAndAtletaId(Long inscricaoEquipeId, Long atletaId);

    boolean existsByInscricaoEquipeIdAndNumeroCamisa(Long inscricaoEquipeId, String numeroCamisa);

    boolean existsByInscricaoEquipeCompeticaoIdAndAtletaId(Long competicaoId, Long atletaId);
}
