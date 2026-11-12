package br.gov.quixada.sports.athlete;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import br.gov.quixada.sports.athlete.dto.AthleteCreateRequest;
import br.gov.quixada.sports.athlete.dto.AthleteResponse;
import br.gov.quixada.sports.shared.Address;
import br.gov.quixada.sports.shared.Gender;
import br.gov.quixada.sports.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("AthleteController — fixed Clock")
class AthleteControllerClockTest {

    private static final String ATHLETE_JSON = """
            {
              "fullName": "Francisco da Silva",
              "cpf": "52998224725",
              "birthDate": "2000-06-15",
              "address": {
                "street": "A Street",
                "number": "10",
                "neighborhood": "Downtown",
                "city": "Quixadá",
                "postalCode": "63900-000"
              },
              "gender": "MALE",
              "phone": "88999999999"
            }
            """;

    @Mock
    private AthleteService service;

    @Mock
    private AthleteMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new AthleteController(service, mapper, ControllerTestSupport.FIXED_CLOCK);
        mockMvc = ControllerTestSupport.mockMvc(controller);
    }

    private Athlete anAthlete() {
        return Athlete.builder()
                .id(1L)
                .fullName("Francisco da Silva")
                .cpf("52998224725")
                .birthDate(LocalDate.of(2000, 6, 15))
                .address(new Address("A Street", "10", "Downtown", "Quixadá", "63900-000"))
                .phone("88999999999")
                .gender(Gender.MALE)
                .status(AthleteStatus.ACTIVE)
                .build();
    }

    private AthleteResponse aResponse() {
        return new AthleteResponse(1L, "Francisco da Silva", "52998224725",
                LocalDate.of(2000, 6, 15), 26, null, "88999999999",
                Gender.MALE, AthleteStatus.ACTIVE, null);
    }

    @Test
    @DisplayName("should calculate the age using the fixed Clock on GET")
    void shouldCalculateAgeWithFixedClockOnGet() throws Exception {
        var athlete = anAthlete();
        when(service.findByIdOrThrowNotFound(1L)).thenReturn(athlete);
        when(mapper.toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK)).thenReturn(aResponse());

        mockMvc.perform(get("/v1/athletes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(26));

        verify(mapper).toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK);
    }

    @Test
    @DisplayName("should calculate the age using the fixed Clock on POST")
    void shouldCalculateAgeWithFixedClockOnPost() throws Exception {
        var athlete = anAthlete();
        when(mapper.toEntity(any(AthleteCreateRequest.class))).thenReturn(athlete);
        when(service.save(athlete)).thenReturn(athlete);
        when(mapper.toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK)).thenReturn(aResponse());

        mockMvc.perform(post("/v1/athletes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ATHLETE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/v1/athletes/1")))
                .andExpect(jsonPath("$.age").value(26));

        verify(mapper).toGetResponse(athlete, ControllerTestSupport.FIXED_CLOCK);
    }
}
