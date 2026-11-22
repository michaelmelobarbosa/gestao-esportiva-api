package br.gov.quixada.sports.club;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import br.gov.quixada.sports.club.dto.ClubCreateRequest;
import br.gov.quixada.sports.club.dto.ClubUpdateRequest;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.AddressMapperImpl;
import br.gov.quixada.sports.shared.AddressRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ClubMapper")
class ClubMapperTest {

    private ClubMapper mapper;

    @BeforeEach
    void setUp() {
        var impl = new ClubMapperImpl();
        ReflectionTestUtils.setField(impl, "addressMapper", new AddressMapperImpl());
        mapper = impl;
    }

    private Club aClub() {
        return Club.builder()
                .id(1L)
                .name("Quixadá Esporte Clube")
                .manager("Carlos Lima")
                .phone("88999999999")
                .address(new Address("Main Avenue", "100", "Downtown", "Quixadá", "63900-000"))
                .status(ClubStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("toEntity(ClubCreateRequest) should map fields and nested address while ignoring id and status")
    void createRequestToEntity() {
        var request = new ClubCreateRequest(
                "Quixadá Esporte Clube",
                "Carlos Lima",
                "88999999999",
                new AddressRequest("Main Avenue", "100", "Downtown", "Quixadá", "63900-000")
        );

        Club entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getName()).isEqualTo("Quixadá Esporte Clube");
        assertThat(entity.getManager()).isEqualTo("Carlos Lima");
        assertThat(entity.getPhone()).isEqualTo("88999999999");
        assertThat(entity.getAddress().getCity()).isEqualTo("Quixadá");
    }

    @Test
    @DisplayName("toEntity(ClubUpdateRequest) should map fields and ignore id and status")
    void updateRequestToEntity() {
        var request = new ClubUpdateRequest(
                "Updated Club",
                "Ana Souza",
                "88888888888",
                new AddressRequest("Second Street", "200", "Center", "Fortaleza", "60000-000")
        );

        Club entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getName()).isEqualTo("Updated Club");
        assertThat(entity.getAddress().getCity()).isEqualTo("Fortaleza");
    }

    @Test
    @DisplayName("toResponse, toSummary, and toSummaryList should map club fields")
    void toResponseAndSummaryShouldMapFields() {
        var club = aClub();

        var response = mapper.toResponse(club);
        var summary = mapper.toSummary(club);
        var list = mapper.toSummaryList(List.of(club));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.address().postalCode()).isEqualTo("63900-000");
        assertThat(response.status()).isEqualTo(ClubStatus.ACTIVE);

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.name()).isEqualTo("Quixadá Esporte Clube");
        assertThat(list).containsExactly(summary);
    }

    @Test
    @DisplayName("should return null when input is null")
    void nullInputsShouldReturnNull() {
        assertThat(mapper.toEntity((ClubCreateRequest) null)).isNull();
        assertThat(mapper.toEntity((ClubUpdateRequest) null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toSummaryList(null)).isNull();
    }
}
