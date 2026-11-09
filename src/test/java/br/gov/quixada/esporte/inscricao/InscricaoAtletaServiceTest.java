package br.gov.quixada.esporte.inscricao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
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
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.equipe.Equipe;
import br.gov.quixada.esporte.inscricao.exception.AtletaForaDaCategoriaException;
import br.gov.quixada.esporte.inscricao.exception.AtletaJaInscritoException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoAtletaNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.UltimoAtletaAtivoException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoInvalidaException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.NumeroCamisaJaUtilizadoException;
import br.gov.quixada.esporte.inscricao.exception.TransicaoStatusInvalidaException;
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
                .status(br.gov.quixada.esporte.atleta.StatusAtleta.ATIVO)
                .build();
    }

    private Modalidade umaModalidade(Long id) {
        return Modalidade.builder().id(id).nome("Futebol").build();
    }

    private Equipe umaEquipe(Long id, Long modalidadeId) {
        return Equipe.builder().id(id).nome("Equipe A").modalidade(umaModalidade(modalidadeId)).build();
    }

    private Categoria umaCategoria(Long id, Integer idadeMinima, Integer idadeMaxima) {
        Competicao competicao = Competicao.builder().id(1L).nome("Copa Teste")
                .status(StatusCompeticao.INSCRICOES_ABERTAS).build();
        return Categoria.builder()
                .id(id)
                .nome("Sub-20")
                .idadeMinima(idadeMinima)
                .idadeMaxima(idadeMaxima)
                .competicao(competicao)
                .status(br.gov.quixada.esporte.categoria.StatusCategoria.ATIVO)
                .build();
    }

    private InscricaoEquipe umaInscricaoEquipe(Categoria categoria) {
        return InscricaoEquipe.builder()
                .id(1L)
                .competicao(Competicao.builder().id(1L).nome("Copa Teste")
                        .status(StatusCompeticao.INSCRICOES_ABERTAS).build())
                .status(StatusInscricao.PENDENTE)
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

            Page<InscricaoAtleta> resultado = service.findAll(null, null, PAGEABLE);

            assertThat(resultado).isSameAs(esperado);
            verify(repository).findAll(PAGEABLE);
        }

        @Test
        @DisplayName("deve filtrar por inscrição de equipe quando informado")
        void deveFiltrarPorInscricaoEquipeQuandoInformado() {
            Page<InscricaoAtleta> esperado = Page.empty();
            when(repository.findByInscricaoEquipeId(1L, PAGEABLE)).thenReturn(esperado);

            Page<InscricaoAtleta> resultado = service.findAll(1L, null, PAGEABLE);

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
            when(repository.existsByInscricaoEquipeIdAndAtletaIdAndStatusIn(1L, 2L, StatusInscricao.OCUPAM_VAGA)).thenReturn(true);
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
            when(repository.existsByInscricaoEquipeCompeticaoIdAndAtletaIdAndStatusIn(1L, 2L, StatusInscricao.OCUPAM_VAGA)).thenReturn(true);
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
            when(repository.existsByInscricaoEquipeIdAndNumeroCamisaAndStatusIn(1L, "10", StatusInscricao.OCUPAM_VAGA)).thenReturn(true);
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
        @DisplayName("deve bloquear transição a partir de CANCELADA")
        void deveBloquearTransicaoDeCancelada() {
            InscricaoAtleta inscricao = InscricaoAtleta.builder().status(StatusInscricao.CANCELADA).build();
            when(repository.findById(1L)).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.APROVADA))
                    .isInstanceOf(TransicaoStatusInvalidaException.class);
        }

        @Test
        @DisplayName("deve lançar InscricaoAtletaNotFoundException quando o id não existir")
        void deveLancarNotFoundQuandoInexistente() {
            assertThatThrownBy(() -> service.alterarStatus(1L, StatusInscricao.APROVADA))
                    .isInstanceOf(InscricaoAtletaNotFoundException.class)
                    .hasMessage("Inscrição de atleta não encontrada");
        }
    }

    @Nested
    @DisplayName("regras de negócio")
    class RegrasDeNegocio {

        @Test
        @DisplayName("deve bloquear inscrição de atleta inativo")
        void deveBloquearAtletaInativo() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 18, 40));
            Atleta atleta = Atleta.builder()
                    .nomeCompleto("Fulano")
                    .dataNascimento(LocalDate.of(2000, 6, 15))
                    .status(br.gov.quixada.esporte.atleta.StatusAtleta.INATIVO)
                    .build();
            when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(inscricaoEquipe));
            when(atletaRepository.findById(2L)).thenReturn(Optional.of(atleta));

            assertThatThrownBy(() -> service.save(InscricaoAtleta.builder().numeroCamisa("10").build(), 1L, 2L))
                    .isInstanceOf(InscricaoInvalidaException.class)
                    .hasMessage("Atleta inativo não pode ser inscrito");
        }

        @Test
        @DisplayName("deve bloquear inscrição em categoria inativa")
        void deveBloquearCategoriaInativa() {
            Competicao competicao = Competicao.builder().id(1L).nome("Copa")
                    .status(StatusCompeticao.INSCRICOES_ABERTAS).build();
            Categoria categoria = Categoria.builder()
                    .id(2L).nome("Sub-20").idadeMinima(18).idadeMaxima(40)
                    .competicao(competicao)
                    .status(br.gov.quixada.esporte.categoria.StatusCategoria.INATIVO)
                    .build();
            InscricaoEquipe inscricaoEquipe = InscricaoEquipe.builder()
                    .id(1L).competicao(competicao).categoria(categoria)
                    .equipe(umaEquipe(3L, 10L)).status(StatusInscricao.PENDENTE)
                    .build();
            Atleta atleta = umAtleta(LocalDate.of(2000, 6, 15));
            when(inscricaoEquipeRepository.findById(1L)).thenReturn(Optional.of(inscricaoEquipe));
            when(atletaRepository.findById(2L)).thenReturn(Optional.of(atleta));

            assertThatThrownBy(() -> service.save(InscricaoAtleta.builder().numeroCamisa("10").build(), 1L, 2L))
                    .isInstanceOf(InscricaoInvalidaException.class)
                    .hasMessage("Categoria inativa não pode receber inscrições");
        }

        @Test
        @DisplayName("não deve remover o último atleta ativo de equipe aprovada")
        void naoDeveRemoverUltimoAtletaAtivo() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 18, 40));
            inscricaoEquipe.alterarStatus(StatusInscricao.APROVADA);
            Atleta atleta = umAtleta(LocalDate.of(2000, 6, 15));
            InscricaoAtleta inscricao = InscricaoAtleta.builder()
                    .id(10L).status(StatusInscricao.APROVADA)
                    .inscricaoEquipe(inscricaoEquipe).atleta(atleta).numeroCamisa("10")
                    .build();
            when(repository.findById(10L)).thenReturn(Optional.of(inscricao));
            when(repository.countByInscricaoEquipeIdAndStatusIn(eq(1L), anyCollection())).thenReturn(1L);

            assertThatThrownBy(() -> service.alterarStatus(10L, StatusInscricao.CANCELADA))
                    .isInstanceOf(UltimoAtletaAtivoException.class);
        }

        @Test
        @DisplayName("deve permitir remover atleta quando há outro ativo na equipe")
        void devePermitirRemoverQuandoHaOutroAtivo() {
            InscricaoEquipe inscricaoEquipe = umaInscricaoEquipe(umaCategoria(2L, 18, 40));
            inscricaoEquipe.alterarStatus(StatusInscricao.APROVADA);
            Atleta atleta = umAtleta(LocalDate.of(2000, 6, 15));
            InscricaoAtleta inscricao = InscricaoAtleta.builder()
                    .id(10L).status(StatusInscricao.APROVADA)
                    .inscricaoEquipe(inscricaoEquipe).atleta(atleta).numeroCamisa("10")
                    .build();
            when(repository.findById(10L)).thenReturn(Optional.of(inscricao));
            when(repository.countByInscricaoEquipeIdAndStatusIn(eq(1L), anyCollection())).thenReturn(2L);

            var resultado = service.alterarStatus(10L, StatusInscricao.CANCELADA);

            assertThat(resultado.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        }
    }
}
