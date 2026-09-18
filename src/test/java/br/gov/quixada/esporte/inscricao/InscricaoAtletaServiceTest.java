package br.gov.quixada.esporte.inscricao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import br.gov.quixada.esporte.atleta.Atleta;
import br.gov.quixada.esporte.atleta.AtletaRepository;
import br.gov.quixada.esporte.atleta.exception.AtletaNotFoundException;
import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.equipe.Equipe;
import br.gov.quixada.esporte.inscricao.exception.AtletaForaDaCategoriaException;
import br.gov.quixada.esporte.inscricao.exception.AtletaJaInscritoException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoAtletaNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.NumeroCamisaJaUtilizadoException;
import br.gov.quixada.esporte.modalidade.Modalidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("InscricaoAtletaService")
class InscricaoAtletaServiceTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);
    private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

    @Mock
    private InscricaoAtletaRepository repository;

    @Mock
    private InscricaoEquipeRepository inscricaoEquipeRepository;

    @Mock
    private AtletaRepository atletaRepository;

    private Clock clock;

    private InscricaoAtletaService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZONE);
        service = new InscricaoAtletaService(repository, inscricaoEquipeRepository, atletaRepository, clock);
    }

    private Atleta umAtleta(LocalDate dataNascimento) {
        return Atleta.builder()
                .nomeCompleto("Fulano de Tal")
                .dataNascimento(dataNascimento)
                .build();
    }

    private Modalidade umaModalidade(Long id) {
        return Modalidade.builder().id(id).nome("Futebol").build();
    }

    private Equipe umaEquipe(Long id, Long modalidadeId) {
        return Equipe.builder().id(id).nome("Equipe A").modalidade(umaModalidade(modalidadeId)).build();
    }

    private Categoria umaCategoria(Long id, Integer idadeMinima, Integer idadeMaxima) {
        Competicao competicao = Competicao.builder().id(1L).nome("Copa Teste").build();
        return Categoria.builder()
                .id(id)
                .nome("Sub-20")
                .idadeMinima(idadeMinima)
                .idadeMaxima(idadeMaxima)
                .competicao(competicao)
                .build();
    }

    private InscricaoEquipe umaInscricaoEquipe(Categoria categoria) {
        return InscricaoEquipe.builder()
                .id(1L)
                .competicao(Competicao.builder().id(1L).nome("Copa Teste").build())
                .categoria(categoria)
                .equipe(umaEquipe(3L, 10L))
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("deve buscar todas quando o id da inscrição de equipe for nulo")
        void deveBuscarTodasQuandoInscricaoEquipeNula() {
            Page<InscricaoAtleta> esperado = Page.empty();
            when(repository.findAll(PAGEABLE)).thenReturn(esperado);

            Page<InscricaoAtleta> resultado = service.findAll(null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
        }

        @Test
        @DisplayName("deve filtrar por inscrição de equipe quando informado")
        void deveFiltrarPorInscricaoEquipeQuandoInformado() {
            Page<InscricaoAtleta> esperado = Page.empty();
            when(repository.findByInscricaoEquipeId(1L, PAGEABLE)).thenReturn(esperado);

            Page<InscricaoAtleta> resultado = service.findAll(1L, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findByInscricaoEquipeId(1L, PAGEABLE);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findByIdOrThrowNotFound")
    class FindByIdOrThrowNotFound {

        @Test
        @DisplayName("deve retornar a inscrição quando existir")
        void deveRetornarInscricaoQuandoExistir() {
            InscricaoAtleta inscricao = InscricaoAtleta.builder().build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            InscricaoAtleta resultado = service.findByIdOrThrowNotFound(1L);

            assertThat(resultado).isSameAs(inscricao);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("deve lançar InscricaoAtletaNotFoundException quando não existir")
        void deveLancarNotFoundQuandoNaoExistir() {
            assertThatThrownBy(() -> service.findByIdOrThrowNotFound(99L))
                    .isInstanceOf(InscricaoAtletaNotFoundException.class)
                    .hasMessage("Inscrição de atleta não encontrada");
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve vincular inscrição e atleta, definir status PENDENTE e salvar")
        void deveSalvarQuandoDadosValidos() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 18, 40));
            Atleta atleta = umAtleta(LocalDate.of(2000, 6, 15));
            when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(inscricaoEquipe));
            when(atletaRepository.findById(2L)).thenReturn(Optional.of(atleta));
            when(repository.save(any(InscricaoAtleta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            InscricaoAtleta inscricao = InscricaoAtleta.builder().numeroCamisa("10").build();
            InscricaoAtleta salva = service.save(inscricao, 1L, 2L);

            assertThat(salva).isSameAs(inscricao);
            assertThat(salva.getInscricaoEquipe()).isSameAs(inscricaoEquipe);
            assertThat(salva.getAtleta()).isSameAs(atleta);
            assertThat(salva.getStatus()).isEqualTo(StatusInscricao.PENDENTE);
            verify(repository).save(inscricao);
        }

        @Test
        @DisplayName("deve lançar InscricaoEquipeNotFoundException quando a inscrição de equipe não existir")
        void deveLancarNotFoundQuandoInscricaoEquipeNaoExistir() {
            InscricaoAtleta inscricao = InscricaoAtleta.builder().numeroCamisa("10").build();

            assertThatThrownBy(() -> service.save(inscricao, 99L, 2L))
                    .isInstanceOf(InscricaoEquipeNotFoundException.class)
                    .hasMessage("Inscrição de equipe não encontrada");
        }

        @Test
        @DisplayName("deve lançar AtletaNotFoundException quando o atleta não existir")
        void deveLancarNotFoundQuandoAtletaNaoExistir() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 18, 40));
            when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(inscricaoEquipe));
            InscricaoAtleta inscricao = InscricaoAtleta.builder().numeroCamisa("10").build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 99L))
                    .isInstanceOf(AtletaNotFoundException.class)
                    .hasMessage("Atleta não encontrado");
        }

        @Test
        @DisplayName("deve lançar AtletaJaInscritoException quando o atleta já estiver nesta equipe")
        void deveLancarJaInscritoQuandoMesmaEquipe() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 18, 40));
            Atleta atleta = umAtleta(LocalDate.of(2000, 6, 15));
            when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(inscricaoEquipe));
            when(atletaRepository.findById(2L)).thenReturn(Optional.of(atleta));
            when(repository.existsByInscricaoEquipeIdAndAtletaId(1L, 2L)).thenReturn(true);
            InscricaoAtleta inscricao = InscricaoAtleta.builder().numeroCamisa("10").build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 2L))
                    .isInstanceOf(AtletaJaInscritoException.class)
                    .hasMessage("Atleta já está inscrito nesta equipe");
        }

        @Test
        @DisplayName("deve lançar AtletaJaInscritoException quando o atleta já estiver em outra equipe da competição")
        void deveLancarJaInscritoQuandoOutraEquipeDaCompeticao() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 18, 40));
            Atleta atleta = umAtleta(LocalDate.of(2000, 6, 15));
            when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(inscricaoEquipe));
            when(atletaRepository.findById(2L)).thenReturn(Optional.of(atleta));
            when(repository.existsByInscricaoEquipeCompeticaoIdAndAtletaId(1L, 2L)).thenReturn(true);
            InscricaoAtleta inscricao = InscricaoAtleta.builder().numeroCamisa("10").build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 2L))
                    .isInstanceOf(AtletaJaInscritoException.class)
                    .hasMessage("Atleta já está inscrito em outra equipe desta competição");
        }

        @Test
        @DisplayName("deve lançar NumeroCamisaJaUtilizadoException quando o número da camisa já estiver em uso")
        void deveLancarNumeroCamisaJaUtilizado() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 18, 40));
            Atleta atleta = umAtleta(LocalDate.of(2000, 6, 15));
            when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(inscricaoEquipe));
            when(atletaRepository.findById(2L)).thenReturn(Optional.of(atleta));
            when(repository.existsByInscricaoEquipeIdAndNumeroCamisa(1L, "10")).thenReturn(true);
            InscricaoAtleta inscricao = InscricaoAtleta.builder().numeroCamisa("10").build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 2L))
                    .isInstanceOf(NumeroCamisaJaUtilizadoException.class)
                    .hasMessage("Número da camisa já utilizado nesta inscrição");
        }

        @Test
        @DisplayName("deve lançar AtletaForaDaCategoriaException quando a idade estiver fora da faixa")
        void deveLancarForaDaCategoria() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 30, 40));
            Atleta atleta = umAtleta(LocalDate.of(2000, 6, 15));
            when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(inscricaoEquipe));
            when(atletaRepository.findById(2L)).thenReturn(Optional.of(atleta));
            InscricaoAtleta inscricao = InscricaoAtleta.builder().numeroCamisa("10").build();

            assertThatThrownBy(() -> service.save(inscricao, 1L, 2L))
                    .isInstanceOf(AtletaForaDaCategoriaException.class)
                    .hasMessage("Idade do atleta está fora da faixa da categoria");
        }

        @Test
        @DisplayName("deve pular a validação de idade quando o atleta não tiver data de nascimento")
        void deveSalvarQuandoDataNascimentoNula() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 30, 40));
            Atleta atleta = umAtleta(null);
            when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(inscricaoEquipe));
            when(atletaRepository.findById(2L)).thenReturn(Optional.of(atleta));
            when(repository.save(any(InscricaoAtleta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

            InscricaoAtleta inscricao = InscricaoAtleta.builder().numeroCamisa("10").build();
            InscricaoAtleta salva = service.save(inscricao, 1L, 2L);

            assertThat(salva).isSameAs(inscricao);
            assertThat(salva.getStatus()).isEqualTo(StatusInscricao.PENDENTE);
            verify(repository).save(inscricao);
        }
    }

    @Nested
    @DisplayName("alterarStatus")
    class AlterarStatus {

        @Test
        @DisplayName("deve alterar o status e não chamar save")
        void deveAlterarStatus() {
            InscricaoAtleta inscricao = InscricaoAtleta.builder().status(StatusInscricao.PENDENTE).build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            InscricaoAtleta resultado = service.alterarStatus(1L, StatusInscricao.APROVADA);

            assertThat(resultado.getStatus()).isEqualTo(StatusInscricao.APROVADA);
            verify(repository, never()).save(any(InscricaoAtleta.class));
        }

        @Test
        @DisplayName("deve manter o status quando for o mesmo (idempotência)")
        void deveManterStatusQuandoIgual() {
            InscricaoAtleta inscricao = InscricaoAtleta.builder().status(StatusInscricao.APROVADA).build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            InscricaoAtleta resultado = service.alterarStatus(1L, StatusInscricao.APROVADA);

            assertThat(resultado.getStatus()).isEqualTo(StatusInscricao.APROVADA);
            verify(repository, never()).save(any(InscricaoAtleta.class));
        }

        @Test
        @DisplayName("deve lançar InscricaoAtletaNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoInexistente() {
            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.APROVADA))
                    .isInstanceOf(InscricaoAtletaNotFoundException.class)
                    .hasMessage("Inscrição de atleta não encontrada");
        }
    }
}
