package br.gov.quixada.sports.competition;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import br.gov.quixada.sports.competition.dto.CompetitionCreateRequest;
import br.gov.quixada.sports.competition.dto.CompetitionUpdateRequest;
import br.gov.quixada.sports.modality.Modality;
import br.gov.quixada.sports.modality.ModalityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompetitionMapper")
class CompetitionMapperTest {

    private CompetitionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CompetitionMapperImpl();
    }

    private Competition aCompetition() {
        var modality = Modality.builder()
                .id(10L)
                .name("Futsal")
                .status(ModalityStatus.ACTIVE)
                .build();

        return Competition.builder()
                .id(1L)
                .name("Municipal Cup")
                .year(2026)
                .modality(modality)
                .registrationStartDate(LocalDate.of(2026, 1, 10))
                .registrationEndDate(LocalDate.of(2026, 2, 10))
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .status(CompetitionStatus.CANCELED)
                .notes("Postponed due to venue maintenance")
                .canceledAt(LocalDateTime.of(2026, 2, 15, 14, 30))
                .build();
    }

    @Test
    @DisplayName("toEntity(CompetitionCreateRequest) should map fields and ignore lifecycle/relation fields")
    void createRequestToEntity() {
        var request = new CompetitionCreateRequest(
                "Municipal Cup",
                2026,
                10L,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 6, 30),
                "Initial notes"
        );

        Competition entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getModality()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getCanceledAt()).isNull();
        assertThat(entity.getName()).isEqualTo("Municipal Cup");
        assertThat(entity.getYear()).isEqualTo(2026);
        assertThat(entity.getRegistrationStartDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(entity.getRegistrationEndDate()).isEqualTo(LocalDate.of(2026, 2, 10));
        assertThat(entity.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(entity.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(entity.getNotes()).isEqualTo("Initial notes");
    }

    @Test
    @DisplayName("toEntity(CompetitionUpdateRequest) should not map id, modality, status, notes, or canceledAt")
    void updateRequestToEntity() {
        var request = new CompetitionUpdateRequest(
                "Updated Cup",
                2027,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 1, 31),
                LocalDate.of(2027, 2, 15),
                LocalDate.of(2027, 5, 30)
        );

        Competition entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getModality()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getNotes()).isNull();
        assertThat(entity.getCanceledAt()).isNull();
        assertThat(entity.getName()).isEqualTo("Updated Cup");
        assertThat(entity.getYear()).isEqualTo(2027);
    }

    @Test
    @DisplayName("toResponse should flatten modality fields and preserve notes and canceledAt")
    void toResponseShouldMapModalityAndMetadata() {
        var response = mapper.toResponse(aCompetition());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Municipal Cup");
        assertThat(response.modalityId()).isEqualTo(10L);
        assertThat(response.modalityName()).isEqualTo("Futsal");
        assertThat(response.status()).isEqualTo(CompetitionStatus.CANCELED);
        assertThat(response.notes()).isEqualTo("Postponed due to venue maintenance");
        assertThat(response.canceledAt()).isEqualTo(LocalDateTime.of(2026, 2, 15, 14, 30));
    }

    @Test
    @DisplayName("toSummary and toSummaryList should map summary fields")
    void toSummaryAndListShouldMapFields() {
        var competition = aCompetition();

        var summary = mapper.toSummary(competition);
        var list = mapper.toSummaryList(List.of(competition));

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.modalityId()).isEqualTo(10L);
        assertThat(summary.modalityName()).isEqualTo("Futsal");
        assertThat(summary.status()).isEqualTo(CompetitionStatus.CANCELED);
        assertThat(list).containsExactly(summary);
    }

    @Test
    @DisplayName("should return null when input is null")
    void nullInputsShouldReturnNull() {
        assertThat(mapper.toEntity((CompetitionCreateRequest) null)).isNull();
        assertThat(mapper.toEntity((CompetitionUpdateRequest) null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toSummaryList(null)).isNull();
    }
}
