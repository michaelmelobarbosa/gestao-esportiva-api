package br.gov.quixada.esporte.competicao;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import br.gov.quixada.esporte.competicao.dto.CompeticaoResponse;
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
@DisplayName("CompeticaoController — observação livre")
class CompeticaoControllerTest {

    @Mock
    private CompeticaoService service;

    @Mock
    private CompeticaoMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new CompeticaoController(service, mapper);
        mockMvc = ControllerTestSupport.mockMvc(controller);
    }

    private Competicao umaCompeticao() {
        return Competicao.builder()
                .id(1L)
                .nome("Copa Teste")
                .ano(2026)
                .dataInicioInscricao(LocalDate.of(2026, 1, 1))
                .dataFimInscricao(LocalDate.of(2026, 2, 1))
                .dataInicio(LocalDate.of(2026, 3, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .status(StatusCompeticao.EM_ANDAMENTO)
                .build();
    }

    private CompeticaoResponse umaResponse(String observacao) {
        return new CompeticaoResponse(1L, "Copa Teste", 2026, 10L, "Futebol",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31),
                StatusCompeticao.EM_ANDAMENTO, observacao, null);
    }

    @Test
    @DisplayName("deve gravar a observação informada")
    void deveGravarObservacao() throws Exception {
        var competicao = umaCompeticao();
        when(service.atualizarObservacao(1L, "Fim prorrogado por chuva")).thenReturn(competicao);
        when(mapper.toResponse(competicao)).thenReturn(umaResponse("Fim prorrogado por chuva"));

        mockMvc.perform(patch("/v1/competicoes/1/observacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"Fim prorrogado por chuva\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacao").value("Fim prorrogado por chuva"));

        verify(service).atualizarObservacao(1L, "Fim prorrogado por chuva");
    }

    @Test
    @DisplayName("deve limpar a observação quando o corpo não trouxer o campo")
    void deveLimparObservacao() throws Exception {
        var competicao = umaCompeticao();
        when(service.atualizarObservacao(1L, null)).thenReturn(competicao);
        when(mapper.toResponse(competicao)).thenReturn(umaResponse(null));

        mockMvc.perform(patch("/v1/competicoes/1/observacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacao").doesNotExist());

        verify(service).atualizarObservacao(1L, null);
    }

    @Test
    @DisplayName("deve rejeitar observação acima de 500 caracteres")
    void deveRejeitarObservacaoLonga() throws Exception {
        var texto = "a".repeat(501);

        mockMvc.perform(patch("/v1/competicoes/1/observacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"" + texto + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("observacao"));

        verifyNoInteractions(service);
    }
}
