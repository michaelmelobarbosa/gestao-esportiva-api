package br.gov.quixada.sports.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import br.gov.quixada.sports.category.dto.CategoryCreateRequest;
import br.gov.quixada.sports.category.dto.CategoryUpdateRequest;
import br.gov.quixada.sports.competition.Competition;
import br.gov.quixada.sports.competition.CompetitionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CategoryMapper")
class CategoryMapperTest {

    private CategoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CategoryMapperImpl();
    }

    private Category aCategory() {
        var competition = Competition.builder()
                .id(100L)
                .name("Municipal Cup")
                .status(CompetitionStatus.PLANNING)
                .build();

        return Category.builder()
                .id(1L)
                .name("U-17")
                .minAge(15)
                .maxAge(17)
                .competition(competition)
                .status(CategoryStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("toEntity(CategoryCreateRequest) should map fields and ignore id, competition, and status")
    void createRequestToEntity() {
        var request = new CategoryCreateRequest("U-17", 15, 17, 100L);

        Category entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCompetition()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getName()).isEqualTo("U-17");
        assertThat(entity.getMinAge()).isEqualTo(15);
        assertThat(entity.getMaxAge()).isEqualTo(17);
    }

    @Test
    @DisplayName("toEntity(CategoryUpdateRequest) should map fields and ignore id, competition, and status")
    void updateRequestToEntity() {
        var request = new CategoryUpdateRequest("U-18", 16, 18);

        Category entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCompetition()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getName()).isEqualTo("U-18");
        assertThat(entity.getMinAge()).isEqualTo(16);
        assertThat(entity.getMaxAge()).isEqualTo(18);
    }

    @Test
    @DisplayName("toResponse, toSummary, and toSummaryList should map competition fields")
    void toResponseAndSummaryShouldFlattenCompetition() {
        var category = aCategory();

        var response = mapper.toResponse(category);
        var summary = mapper.toSummary(category);
        var list = mapper.toSummaryList(List.of(category));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.competitionId()).isEqualTo(100L);
        assertThat(response.competitionName()).isEqualTo("Municipal Cup");
        assertThat(response.status()).isEqualTo(CategoryStatus.ACTIVE);

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.competitionId()).isEqualTo(100L);
        assertThat(list).containsExactly(summary);
    }

    @Test
    @DisplayName("should return null when input is null")
    void nullInputsShouldReturnNull() {
        assertThat(mapper.toEntity((CategoryCreateRequest) null)).isNull();
        assertThat(mapper.toEntity((CategoryUpdateRequest) null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toSummaryList(null)).isNull();
    }
}
