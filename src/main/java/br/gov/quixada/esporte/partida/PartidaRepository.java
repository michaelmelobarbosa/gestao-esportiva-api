package br.gov.quixada.esporte.partida;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartidaRepository extends JpaRepository<Partida, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "inscricaoMandante", "inscricaoVisitante",
            "inscricaoMandante.equipe", "inscricaoVisitante.equipe",
            "inscricaoMandante.competicao", "inscricaoVisitante.competicao",
            "inscricaoMandante.categoria", "inscricaoVisitante.categoria"})
    Page<Partida> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "inscricaoMandante", "inscricaoVisitante",
            "inscricaoMandante.equipe", "inscricaoVisitante.equipe",
            "inscricaoMandante.competicao", "inscricaoVisitante.competicao",
            "inscricaoMandante.categoria", "inscricaoVisitante.categoria"})
    Page<Partida> findByStatus(StatusPartida status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "inscricaoMandante", "inscricaoVisitante",
            "inscricaoMandante.equipe", "inscricaoVisitante.equipe",
            "inscricaoMandante.competicao", "inscricaoVisitante.competicao",
            "inscricaoMandante.categoria", "inscricaoVisitante.categoria"})
    @Query("""
            select p from Partida p
            where p.inscricaoMandante.competicao.id = :competicaoId
              and p.inscricaoMandante.categoria.id = :categoriaId
            """)
    Page<Partida> findByCompeticaoAndCategoria(@Param("competicaoId") Long competicaoId,
                                               @Param("categoriaId") Long categoriaId,
                                               Pageable pageable);

    @EntityGraph(attributePaths = {
            "inscricaoMandante", "inscricaoVisitante",
            "inscricaoMandante.equipe", "inscricaoVisitante.equipe",
            "inscricaoMandante.competicao", "inscricaoVisitante.competicao",
            "inscricaoMandante.categoria", "inscricaoVisitante.categoria"})
    @Query("""
            select p from Partida p
            where p.inscricaoMandante.competicao.id = :competicaoId
              and p.inscricaoMandante.categoria.id = :categoriaId
              and p.status = :status
            """)
    Page<Partida> findByCompeticaoAndCategoriaAndStatus(@Param("competicaoId") Long competicaoId,
                                                        @Param("categoriaId") Long categoriaId,
                                                        @Param("status") StatusPartida status,
                                                        Pageable pageable);

    @EntityGraph(attributePaths = {
            "inscricaoMandante", "inscricaoVisitante",
            "inscricaoMandante.equipe", "inscricaoVisitante.equipe",
            "inscricaoMandante.competicao", "inscricaoVisitante.competicao",
            "inscricaoMandante.categoria", "inscricaoVisitante.categoria"})
    @Query("""
            select p from Partida p
            where p.inscricaoMandante.competicao.id = :competicaoId
              and p.inscricaoMandante.categoria.id = :categoriaId
            """)
    List<Partida> findByCompeticaoAndCategoria(@Param("competicaoId") Long competicaoId,
                                               @Param("categoriaId") Long categoriaId);

    @EntityGraph(attributePaths = {
            "inscricaoMandante", "inscricaoVisitante",
            "inscricaoMandante.equipe", "inscricaoVisitante.equipe",
            "inscricaoMandante.competicao", "inscricaoVisitante.competicao",
            "inscricaoMandante.categoria", "inscricaoVisitante.categoria"})
    @Query("""
            select p from Partida p
            where (:competicaoId is null or p.inscricaoMandante.competicao.id = :competicaoId)
              and (:categoriaId is null or p.inscricaoMandante.categoria.id = :categoriaId)
              and (:status is null or p.status = :status)
            """)
    Page<Partida> search(@Param("competicaoId") Long competicaoId,
                         @Param("categoriaId") Long categoriaId,
                         @Param("status") StatusPartida status,
                         Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "inscricaoMandante", "inscricaoVisitante",
            "inscricaoMandante.equipe", "inscricaoVisitante.equipe",
            "inscricaoMandante.competicao", "inscricaoVisitante.competicao",
            "inscricaoMandante.categoria", "inscricaoVisitante.categoria"})
    Optional<Partida> findById(Long id);

    @Query("""
            select (count(p) > 0) from Partida p
            where p.id <> :idIgnorado
              and p.rodada = :rodada
              and p.status <> :cancelada
              and ((p.inscricaoMandante.id = :mandante and p.inscricaoVisitante.id = :visitante)
                or (p.inscricaoMandante.id = :visitante and p.inscricaoVisitante.id = :mandante))
            """)
    boolean existsConfrontoNaRodada(@Param("mandante") Long mandanteId,
                                    @Param("visitante") Long visitanteId,
                                    @Param("rodada") Integer rodada,
                                    @Param("idIgnorado") Long idIgnorado,
                                    @Param("cancelada") StatusPartida cancelada);

    @Query("""
            select (count(p) > 0) from Partida p
            where p.id <> :idIgnorado
              and p.status <> :cancelada
              and p.dataHora = :dataHora
              and (p.inscricaoMandante.id in :inscricaoIds or p.inscricaoVisitante.id in :inscricaoIds)
            """)
    boolean existsConflitoDataHora(@Param("inscricaoIds") Collection<Long> inscricaoIds,
                                   @Param("dataHora") LocalDateTime dataHora,
                                   @Param("idIgnorado") Long idIgnorado,
                                   @Param("cancelada") StatusPartida cancelada);

    @Query("""
            select (count(p) > 0) from Partida p
            where p.id <> :idIgnorado
              and p.rodada = :rodada
              and p.status <> :cancelada
              and (p.inscricaoMandante.id in :inscricaoIds or p.inscricaoVisitante.id in :inscricaoIds)
            """)
    boolean existsEquipeNaRodada(@Param("inscricaoIds") Collection<Long> inscricaoIds,
                                 @Param("rodada") Integer rodada,
                                 @Param("idIgnorado") Long idIgnorado,
                                 @Param("cancelada") StatusPartida cancelada);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Partida p set p.status = :cancelada, p.dataCancelamento = :dataCancelamento
            where p.status in :statuses
              and (p.inscricaoMandante.competicao.id = :competicaoId
                or p.inscricaoVisitante.competicao.id = :competicaoId)
            """)
    int cancelarPorCompeticao(@Param("competicaoId") Long competicaoId,
                              @Param("statuses") Collection<StatusPartida> statuses,
                              @Param("cancelada") StatusPartida cancelada,
                              @Param("dataCancelamento") LocalDateTime dataCancelamento);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Partida p set p.status = :cancelada, p.dataCancelamento = :dataCancelamento
            where p.status in :statuses
              and (p.inscricaoMandante.id = :inscricaoId or p.inscricaoVisitante.id = :inscricaoId)
            """)
    int cancelarPorInscricao(@Param("inscricaoId") Long inscricaoId,
                             @Param("statuses") Collection<StatusPartida> statuses,
                             @Param("cancelada") StatusPartida cancelada,
                             @Param("dataCancelamento") LocalDateTime dataCancelamento);
}
