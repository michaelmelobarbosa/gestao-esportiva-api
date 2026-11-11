package br.gov.quixada.esporte.atleta;

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

import br.gov.quixada.esporte.atleta.dto.AtletaCreateRequest;
import br.gov.quixada.esporte.atleta.dto.AtletaResponse;
import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import br.gov.quixada.esporte.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@DisplayName("AtletaController — Clock fixo")
class AtletaControllerClockTest {

    private static final String ATLETA_JSON = """
            {
              "nomeCompleto": "Francisco da Silva",
              "cpf": "52998224725",
              "dataNascimento": "2000-06-15",
              "endereco": {
                "logradouro": "Rua A",
                "numero": "10",
                "bairro": "Centro",
                "cidade": "Quixadá",
                "cep": "63900-000"
              },
              "sexo": "MASCULINO",
              "telefone": "88999999999"
            }
            """;

    @Mock
    private AtletaService service;

    @Mock
    private AtletaMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new AtletaController(service, mapper, ControllerTestSupport.FIXED_CLOCK);
        mockMvc = ControllerTestSupport.mockMvc(controller);
    }

    private Atleta umAtleta() {
        return Atleta.builder()
                .id(1L)
                .nomeCompleto("Francisco da Silva")
                .cpf("52998224725")
                .dataNascimento(LocalDate.of(2000, 6, 15))
                .endereco(new Endereco("Rua A", "10", "Centro", "Quixadá", "63900-000"))
                .telefone("88999999999")
                .sexo(Sexo.MASCULINO)
                .status(StatusAtleta.ATIVO)
                .build();
    }

    private AtletaResponse umaResponse() {
        return new AtletaResponse(1L, "Francisco da Silva", "52998224725",
                LocalDate.of(2000, 6, 15), 26, null, "88999999999",
                Sexo.MASCULINO, StatusAtleta.ATIVO, null);
    }

    @Test
    @DisplayName("deve calcular a idade usando o Clock fixo no GET")
    void deveCalcularIdadeComClockFixoNoGet() throws Exception {
        var atleta = umAtleta();
        when(service.findByIdOrThrowNotFound(1L)).thenReturn(atleta);
        when(mapper.toGetResponse(atleta, ControllerTestSupport.FIXED_CLOCK)).thenReturn(umaResponse());

        mockMvc.perform(get("/v1/atletas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idade").value(26));

        verify(mapper).toGetResponse(atleta, ControllerTestSupport.FIXED_CLOCK);
    }

    @Test
    @DisplayName("deve calcular a idade usando o Clock fixo no POST")
    void deveCalcularIdadeComClockFixoNoPost() throws Exception {
        var atleta = umAtleta();
        when(mapper.toEntity(any(AtletaCreateRequest.class))).thenReturn(atleta);
        when(service.save(atleta)).thenReturn(atleta);
        when(mapper.toGetResponse(atleta, ControllerTestSupport.FIXED_CLOCK)).thenReturn(umaResponse());

        mockMvc.perform(post("/v1/atletas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ATLETA_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/v1/atletas/1")))
                .andExpect(jsonPath("$.idade").value(26));

        verify(mapper).toGetResponse(atleta, ControllerTestSupport.FIXED_CLOCK);
    }
}
