package br.gov.quixada.sports.match;

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
public interface MatchRepository extends JpaRepository<Match, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "homeRegistration", "awayRegistration",
            "homeRegistration.team", "awayRegistration.team",
            "homeRegistration.competition", "awayRegistration.competition",
            "homeRegistration.category", "awayRegistration.category"})
    Page<Match> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "homeRegistration", "awayRegistration",
            "homeRegistration.team", "awayRegistration.team",
            "homeRegistration.competition", "awayRegistration.competition",
            "homeRegistration.category", "awayRegistration.category"})
    Page<Match> findByStatus(MatchStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "homeRegistration", "awayRegistration",
            "homeRegistration.team", "awayRegistration.team",
            "homeRegistration.competition", "awayRegistration.competition",
            "homeRegistration.category", "awayRegistration.category"})
    @Query("""
            select p from Match p
            where p.homeRegistration.competition.id = :competitionId
              and p.homeRegistration.category.id = :categoryId
            """)
    Page<Match> findByCompetitionAndCategory(@Param("competitionId") Long competitionId,
                                               @Param("categoryId") Long categoryId,
                                               Pageable pageable);

    @EntityGraph(attributePaths = {
            "homeRegistration", "awayRegistration",
            "homeRegistration.team", "awayRegistration.team",
            "homeRegistration.competition", "awayRegistration.competition",
            "homeRegistration.category", "awayRegistration.category"})
    @Query("""
            select p from Match p
            where p.homeRegistration.competition.id = :competitionId
              and p.homeRegistration.category.id = :categoryId
              and p.status = :status
            """)
    Page<Match> findByCompetitionAndCategoryAndStatus(@Param("competitionId") Long competitionId,
                                                        @Param("categoryId") Long categoryId,
                                                        @Param("status") MatchStatus status,
                                                        Pageable pageable);

    @EntityGraph(attributePaths = {
            "homeRegistration", "awayRegistration",
            "homeRegistration.team", "awayRegistration.team",
            "homeRegistration.competition", "awayRegistration.competition",
            "homeRegistration.category", "awayRegistration.category"})
    @Query("""
            select p from Match p
            where p.homeRegistration.competition.id = :competitionId
              and p.homeRegistration.category.id = :categoryId
            """)
    List<Match> findByCompetitionAndCategory(@Param("competitionId") Long competitionId,
                                               @Param("categoryId") Long categoryId);

    @EntityGraph(attributePaths = {
            "homeRegistration", "awayRegistration",
            "homeRegistration.team", "awayRegistration.team",
            "homeRegistration.competition", "awayRegistration.competition",
            "homeRegistration.category", "awayRegistration.category"})
    @Query("""
            select p from Match p
            where (:competitionId is null or p.homeRegistration.competition.id = :competitionId)
              and (:categoryId is null or p.homeRegistration.category.id = :categoryId)
              and (:status is null or p.status = :status)
            """)
    Page<Match> search(@Param("competitionId") Long competitionId,
                         @Param("categoryId") Long categoryId,
                         @Param("status") MatchStatus status,
                         Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "homeRegistration", "awayRegistration",
            "homeRegistration.team", "awayRegistration.team",
            "homeRegistration.competition", "awayRegistration.competition",
            "homeRegistration.category", "awayRegistration.category"})
    Optional<Match> findById(Long id);

    @Query("""
            select (count(p) > 0) from Match p
            where p.id <> :ignoredId
              and p.round = :round
              and p.status <> :canceled
              and ((p.homeRegistration.id = :home and p.awayRegistration.id = :away)
                or (p.homeRegistration.id = :away and p.awayRegistration.id = :home))
            """)
    boolean existsConfrontoNaRound(@Param("home") Long homeId,
                                    @Param("away") Long awayId,
                                    @Param("round") Integer round,
                                    @Param("ignoredId") Long ignoredId,
                                    @Param("canceled") MatchStatus canceled);

    @Query("""
            select (count(p) > 0) from Match p
            where p.id <> :ignoredId
              and p.status <> :canceled
              and p.scheduledAt = :scheduledAt
              and (p.homeRegistration.id in :registrationIds or p.awayRegistration.id in :registrationIds)
            """)
    boolean existsConflitoScheduledAt(@Param("registrationIds") Collection<Long> registrationIds,
                                   @Param("scheduledAt") LocalDateTime scheduledAt,
                                   @Param("ignoredId") Long ignoredId,
                                   @Param("canceled") MatchStatus canceled);

    @Query("""
            select (count(p) > 0) from Match p
            where p.id <> :ignoredId
              and p.round = :round
              and p.status <> :canceled
              and (p.homeRegistration.id in :registrationIds or p.awayRegistration.id in :registrationIds)
            """)
    boolean existsTeamNaRound(@Param("registrationIds") Collection<Long> registrationIds,
                                 @Param("round") Integer round,
                                 @Param("ignoredId") Long ignoredId,
                                 @Param("canceled") MatchStatus canceled);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Match p set p.status = :canceled, p.canceledAt = :canceledAt
            where p.status in :statuses
              and (p.homeRegistration.competition.id = :competitionId
                or p.awayRegistration.competition.id = :competitionId)
            """)
    int cancelByCompetition(@Param("competitionId") Long competitionId,
                              @Param("statuses") Collection<MatchStatus> statuses,
                              @Param("canceled") MatchStatus canceled,
                              @Param("canceledAt") LocalDateTime canceledAt);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Match p set p.status = :canceled, p.canceledAt = :canceledAt
            where p.status in :statuses
              and (p.homeRegistration.id = :registrationId or p.awayRegistration.id = :registrationId)
            """)
    int cancelByRegistration(@Param("registrationId") Long registrationId,
                             @Param("statuses") Collection<MatchStatus> statuses,
                             @Param("canceled") MatchStatus canceled,
                             @Param("canceledAt") LocalDateTime canceledAt);
}
