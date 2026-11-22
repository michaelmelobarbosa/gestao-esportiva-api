package br.gov.quixada.sports.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AddressMapper")
class AddressMapperTest {

    private AddressMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AddressMapperImpl();
    }

    @Test
    @DisplayName("toAddress should map AddressRequest fields")
    void toAddressShouldMapFields() {
        var request = new AddressRequest("Main St", "10", "Downtown", "Quixadá", "63900-000");

        Address address = mapper.toAddress(request);

        assertThat(address.getStreet()).isEqualTo("Main St");
        assertThat(address.getNumber()).isEqualTo("10");
        assertThat(address.getNeighborhood()).isEqualTo("Downtown");
        assertThat(address.getCity()).isEqualTo("Quixadá");
        assertThat(address.getPostalCode()).isEqualTo("63900-000");
    }

    @Test
    @DisplayName("toResponse should map Address fields")
    void toResponseShouldMapFields() {
        var address = new Address("Main St", "10", "Downtown", "Quixadá", "63900-000");

        AddressResponse response = mapper.toResponse(address);

        assertThat(response.street()).isEqualTo("Main St");
        assertThat(response.number()).isEqualTo("10");
        assertThat(response.neighborhood()).isEqualTo("Downtown");
        assertThat(response.city()).isEqualTo("Quixadá");
        assertThat(response.postalCode()).isEqualTo("63900-000");
    }

    @Test
    @DisplayName("should return null when input is null")
    void nullInputsShouldReturnNull() {
        assertThat(mapper.toAddress(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
    }
}
