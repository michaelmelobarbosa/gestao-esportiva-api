package br.gov.quixada.sports.modality;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import br.gov.quixada.sports.modality.dto.ModalityCreateRequest;
import br.gov.quixada.sports.modality.dto.ModalityUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModalityMapper")
class ModalityMapperTest {

    private ModalityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ModalityMapperImpl();
    }

    private Modality aModality() {
        return Modality.builder()
                .id(1L)
                .name("Futsal")
                .status(ModalityStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("toEntity(ModalityCreateRequest) should map name and ignore id and status")
    void createRequestToEntity() {
        Modality entity = mapper.toEntity(new ModalityCreateRequest("Futsal"));

        assertThat(entity.getId()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getName()).isEqualTo("Futsal");
    }

    @Test
    @DisplayName("toEntity(ModalityUpdateRequest) should map name and ignore id and status")
    void updateRequestToEntity() {
        Modality entity = mapper.toEntity(new ModalityUpdateRequest("Handball"));

        assertThat(entity.getId()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getName()).isEqualTo("Handball");
    }

    @Test
    @DisplayName("toResponse, toSummary, and toSummaryList should map modality fields")
    void toResponseAndSummaryShouldMapFields() {
        var modality = aModality();

        var response = mapper.toResponse(modality);
        var summary = mapper.toSummary(modality);
        var list = mapper.toSummaryList(List.of(modality));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Futsal");
        assertThat(response.status()).isEqualTo(ModalityStatus.ACTIVE);

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.name()).isEqualTo("Futsal");
        assertThat(list).containsExactly(summary);
    }

    @Test
    @DisplayName("should return null when input is null")
    void nullInputsShouldReturnNull() {
        assertThat(mapper.toEntity((ModalityCreateRequest) null)).isNull();
        assertThat(mapper.toEntity((ModalityUpdateRequest) null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toSummaryList(null)).isNull();
    }
}
