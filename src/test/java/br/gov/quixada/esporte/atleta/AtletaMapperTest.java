package br.gov.quixada.esporte.atleta;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import br.gov.quixada.esporte.atleta.dto.AtletaUpdateRequest;
import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.EnderecoMapperImpl;
import br.gov.quixada.esporte.extras.EnderecoRequest;
import br.gov.quixada.esporte.extras.Sexo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("AtletaMapper")
class AtletaMapperTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Fortaleza"));

    private AtletaMapper mapper;

    @BeforeEach
    void setUp() {
        var impl = new AtletaMapperImpl();
        ReflectionTestUtils.setField(impl, "enderecoMapper", new EnderecoMapperImpl());
        mapper = impl;
    }

    private Atleta umAtleta(LocalDate dataNascimento) {
        return Atleta.builder()
                .id(1L)
                .nomeCompleto("Francisco da Silva")
                .cpf("52998224725")
                .dataNascimento(dataNascimento)
                .endereco(new Endereco("Rua A", "10", "Centro", "Quixadá", "63900-000"))
                .telefone("88999999999")
                .sexo(Sexo.MASCULINO)
                .status(StatusAtleta.ATIVO)
                .build();
    }

    private AtletaUpdateRequest umUpdateRequest() {
        return new AtletaUpdateRequest(
                "Maria Souza",
                LocalDate.of(1990, 5, 5),
                new EnderecoRequest("Rua B", "20", "Bairro Novo", "Fortaleza", "60000-000"),
                Sexo.FEMININO,
                "88888888888"
        );
    }

    @Test
    @DisplayName("calcularIdade deve usar o Clock informado")
    void calcularIdadeDeveUsarClock() {
        assertThat(mapper.calcularIdade(LocalDate.of(2000, 6, 15), CLOCK)).isEqualTo(26);
    }

    @Test
    @DisplayName("calcularIdade deve considerar aniversário ainda não ocorrido")
    void calcularIdadeAntesDoAniversario() {
        assertThat(mapper.calcularIdade(LocalDate.of(2000, 6, 16), CLOCK)).isEqualTo(25);
    }

    @Test
    @DisplayName("calcularIdade deve retornar null quando a data for nula")
    void calcularIdadeComDataNula() {
        assertThat(mapper.calcularIdade(null, CLOCK)).isNull();
    }

    @Test
    @DisplayName("toEntity(AtletaUpdateRequest) não deve mapear o CPF")
    void updateNaoDeveMapearCpf() {
        Atleta atleta = mapper.toEntity(umUpdateRequest());

        assertThat(atleta.getCpf()).isNull();
        assertThat(atleta.getNomeCompleto()).isEqualTo("Maria Souza");
        assertThat(atleta.getSexo()).isEqualTo(Sexo.FEMININO);
        assertThat(atleta.getTelefone()).isEqualTo("88888888888");
        assertThat(atleta.getEndereco().getCidade()).isEqualTo("Fortaleza");
        assertThat(atleta.getStatus()).isNull();
    }

    @Test
    @DisplayName("toEntity(AtletaUpdateRequest) não deve mapear id nem data de cadastro")
    void updateNaoDeveMapearCamposDeCriacao() {
        Atleta atleta = mapper.toEntity(umUpdateRequest());

        assertThat(atleta.getId()).isNull();
        assertThat(atleta.getDataCadastro()).isNull();
    }

    @Test
    @DisplayName("toGetResponse deve calcular a idade com o Clock")
    void toGetResponseDeveCalcularIdade() {
        var response = mapper.toGetResponse(umAtleta(LocalDate.of(2000, 6, 15)), CLOCK);

        assertThat(response.idade()).isEqualTo(26);
        assertThat(response.nomeCompleto()).isEqualTo("Francisco da Silva");
        assertThat(response.endereco().cidade()).isEqualTo("Quixadá");
    }

    @Test
    @DisplayName("toGetResponse deve retornar idade null quando dataNascimento for null")
    void toGetResponseComDataNula() {
        var response = mapper.toGetResponse(umAtleta(null), CLOCK);

        assertThat(response.dataNascimento()).isNull();
        assertThat(response.idade()).isNull();
    }

    @Test
    @DisplayName("toResumo deve retornar idade null quando dataNascimento for null")
    void toResumoComDataNula() {
        var response = mapper.toResumo(umAtleta(null), CLOCK);

        assertThat(response.idade()).isNull();
        assertThat(response.nomeCompleto()).isEqualTo("Francisco da Silva");
    }

    @Test
    @DisplayName("toResumo deve calcular a idade com o Clock")
    void toResumoDeveCalcularIdade() {
        var response = mapper.toResumo(umAtleta(LocalDate.of(2000, 6, 16)), CLOCK);

        assertThat(response.idade()).isEqualTo(25);
    }
}
