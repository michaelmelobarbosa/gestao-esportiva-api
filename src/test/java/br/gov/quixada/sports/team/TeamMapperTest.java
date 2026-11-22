package br.gov.quixada.sports.team;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import br.gov.quixada.sports.club.Club;
import br.gov.quixada.sports.club.ClubStatus;
import br.gov.quixada.sports.modality.Modality;
import br.gov.quixada.sports.modality.ModalityStatus;
import br.gov.quixada.sports.team.dto.TeamCreateRequest;
import br.gov.quixada.sports.team.dto.TeamUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TeamMapper")
class TeamMapperTest {

    private TeamMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TeamMapperImpl();
    }

    private Team aTeam() {
        var club = Club.builder()
                .id(10L)
                .name("Quixadá Club")
                .status(ClubStatus.ACTIVE)
                .build();

        var modality = Modality.builder()
                .id(20L)
                .name("Futsal")
                .status(ModalityStatus.ACTIVE)
                .build();

        return Team.builder()
                .id(1L)
                .name("Lions FC")
                .club(club)
                .modality(modality)
                .manager("Roberto Silva")
                .status(TeamStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("toEntity(TeamCreateRequest) should map fields and ignore id, club, modality, and status")
    void createRequestToEntity() {
        var request = new TeamCreateRequest("Lions FC", 10L, 20L, "Roberto Silva");

        Team entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getClub()).isNull();
        assertThat(entity.getModality()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getName()).isEqualTo("Lions FC");
        assertThat(entity.getManager()).isEqualTo("Roberto Silva");
    }

    @Test
    @DisplayName("toEntity(TeamUpdateRequest) should map fields and ignore id, club, modality, and status")
    void updateRequestToEntity() {
        var request = new TeamUpdateRequest("Lions Futsal", "Pedro Gomes");

        Team entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getClub()).isNull();
        assertThat(entity.getModality()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getName()).isEqualTo("Lions Futsal");
        assertThat(entity.getManager()).isEqualTo("Pedro Gomes");
    }

    @Test
    @DisplayName("toResponse, toSummary, and toSummaryList should flatten club and modality fields")
    void toResponseAndSummaryShouldFlattenRelations() {
        var team = aTeam();

        var response = mapper.toResponse(team);
        var summary = mapper.toSummary(team);
        var list = mapper.toSummaryList(List.of(team));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.clubId()).isEqualTo(10L);
        assertThat(response.clubName()).isEqualTo("Quixadá Club");
        assertThat(response.modalityId()).isEqualTo(20L);
        assertThat(response.modalityName()).isEqualTo("Futsal");
        assertThat(response.status()).isEqualTo(TeamStatus.ACTIVE);

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.clubName()).isEqualTo("Quixadá Club");
        assertThat(summary.modalityName()).isEqualTo("Futsal");
        assertThat(list).containsExactly(summary);
    }

    @Test
    @DisplayName("should return null when input is null")
    void nullInputsShouldReturnNull() {
        assertThat(mapper.toEntity((TeamCreateRequest) null)).isNull();
        assertThat(mapper.toEntity((TeamUpdateRequest) null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toSummaryList(null)).isNull();
    }
}
