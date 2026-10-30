package br.gov.quixada.esporte.atleta;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import br.gov.quixada.esporte.extras.Endereco;
import br.gov.quixada.esporte.extras.Sexo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("Atleta — hooks de persistência")
class AtletaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    private Atleta umAtleta(String nomeCompleto) {
        return Atleta.builder()
                .nomeCompleto(nomeCompleto)
                .cpf("52998224725")
                .dataNascimento(LocalDate.of(2000, 1, 1))
                .endereco(new Endereco("Rua A", "10", "Centro", "Quixadá", "63900-000"))
                .telefone("88999999999")
                .sexo(Sexo.MASCULINO)
                .build();
    }

    @Test
    @DisplayName("@PrePersist deve preencher nomeCompletoNormalizado ao inserir")
    void prePersistDeveNormalizarNome() {
        Atleta salvo = entityManager.persistFlushFind(umAtleta("João Silva"));

        assertThat(salvo.getNomeCompletoNormalizado()).isEqualTo("joaosilva");
    }

    @Test
    @DisplayName("@PreUpdate deve renormalizar nomeCompletoNormalizado ao editar o nome")
    void preUpdateDeveNormalizarNomeAoEditar() {
        Atleta atleta = entityManager.persistFlushFind(umAtleta("João Silva"));

        atleta.atualizarDados(
                "Maria Conceição",
                LocalDate.of(1990, 5, 5),
                new Endereco("Rua B", "20", "Bairro Novo", "Fortaleza", "60000-000"),
                "88888888888",
                Sexo.FEMININO
        );
        entityManager.flush();
        entityManager.clear();

        Atleta recarregado = entityManager.find(Atleta.class, atleta.getId());
        assertThat(recarregado.getNomeCompletoNormalizado()).isEqualTo("mariaconceicao");
    }
}
