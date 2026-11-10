package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.atleta.Atleta;
import br.gov.quixada.esporte.atleta.AtletaRepository;
import br.gov.quixada.esporte.atleta.StatusAtleta;
import br.gov.quixada.esporte.atleta.exception.AtletaNotFoundException;
import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.categoria.StatusCategoria;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.inscricao.exception.AtletaForaDaCategoriaException;
import br.gov.quixada.esporte.inscricao.exception.AtletaJaInscritoException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoAtletaNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoInvalidaException;
import br.gov.quixada.esporte.inscricao.exception.NumeroCamisaJaUtilizadoException;
import br.gov.quixada.esporte.inscricao.exception.TransicaoStatusInvalidaException;
import br.gov.quixada.esporte.inscricao.exception.UltimoAtletaAtivoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InscricaoAtletaService {

    private final InscricaoAtletaRepository repository;
    private final InscricaoEquipeRepository inscricaoEquipeRepository;
    private final AtletaRepository atletaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<InscricaoAtleta> findAll(Long inscricaoEquipeId, StatusInscricao status, Pageable pageable) {
        if (inscricaoEquipeId == null) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByInscricaoEquipeId(inscricaoEquipeId, pageable)
                : repository.findByInscricaoEquipeIdAndStatus(inscricaoEquipeId, status, pageable);
    }

    @Transactional(readOnly = true)
    public InscricaoAtleta findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new InscricaoAtletaNotFoundException("Inscrição de atleta não encontrada"));
    }

    @Transactional
    public InscricaoAtleta save(InscricaoAtleta inscricao, Long inscricaoEquipeId, Long atletaId) {
        InscricaoEquipe inscricaoEquipe = buscarInscricaoEquipe(inscricaoEquipeId);
        Atleta atleta = buscarAtleta(atletaId);

        validarCompeticaoAberta(inscricaoEquipe.getCompeticao());
        validarInscricaoEquipeAtiva(inscricaoEquipe);
        validarAtletaAtivo(atleta);
        validarCategoriaAtiva(inscricaoEquipe.getCategoria());
        validarVagas(inscricaoEquipeId, inscricaoEquipe.getCompeticao().getId(), atletaId,
                inscricao.getNumeroCamisa(), null);
        validarIdade(atleta, inscricaoEquipe.getCategoria());

        inscricao.definirInscricaoEquipe(inscricaoEquipe);
        inscricao.definirAtleta(atleta);
        inscricao.definirStatus(StatusInscricao.PENDENTE);
        return repository.save(inscricao);
    }

    @Transactional
    public InscricaoAtleta alterarStatus(Long id, StatusInscricao status) {
        InscricaoAtleta inscricao = findByIdOrThrowNotFound(id);

        if (!inscricao.getStatus().podeTransicionarPara(status)) {
            throw new TransicaoStatusInvalidaException(
                    "Transição de status inválida: " + inscricao.getStatus() + " -> " + status);
        }

        validarRemocaoUltimoAtleta(inscricao, status);

        if (status == StatusInscricao.PENDENTE) {
            revalidarReativacao(inscricao);
        }

        inscricao.alterarStatus(status, LocalDateTime.now(clock));
        return inscricao;
    }

    /**
     * Campo livre de comunicação: pode ser criado, alterado ou limpo em qualquer
     * status, sem checagens de negócio.
     */
    @Transactional
    public InscricaoAtleta atualizarObservacao(Long id, String observacao) {
        InscricaoAtleta inscricao = findByIdOrThrowNotFound(id);
        inscricao.atualizarObservacao(observacao);
        return inscricao;
    }

    @Transactional
    public InscricaoAtleta update(Long id, String numeroCamisa) {
        InscricaoAtleta inscricao = findByIdOrThrowNotFound(id);

        if (numeroCamisa.equals(inscricao.getNumeroCamisa())) {
            return inscricao;
        }

        Long inscricaoEquipeId = inscricao.getInscricaoEquipe().getId();
        if (repository.existsByInscricaoEquipeIdAndNumeroCamisaAndIdNotAndStatusIn(
                inscricaoEquipeId, numeroCamisa, id, StatusInscricao.OCUPAM_VAGA)) {
            throw new NumeroCamisaJaUtilizadoException("Número da camisa já utilizado nesta inscrição");
        }

        inscricao.atualizarDados(numeroCamisa);
        return inscricao;
    }

    private void revalidarReativacao(InscricaoAtleta inscricao) {
        var inscricaoEquipe = inscricao.getInscricaoEquipe();
        validarCompeticaoAberta(inscricaoEquipe.getCompeticao());
        validarInscricaoEquipeAtiva(inscricaoEquipe);
        validarAtletaAtivo(inscricao.getAtleta());
        validarCategoriaAtiva(inscricaoEquipe.getCategoria());
        validarVagas(inscricaoEquipe.getId(), inscricaoEquipe.getCompeticao().getId(),
                inscricao.getAtleta().getId(), inscricao.getNumeroCamisa(), inscricao.getId());
        validarIdade(inscricao.getAtleta(), inscricaoEquipe.getCategoria());
    }

    private void validarVagas(Long inscricaoEquipeId, Long competicaoId, Long atletaId,
                              String numeroCamisa, Long idIgnorado) {
        boolean atletaDuplicado = idIgnorado == null
                ? repository.existsByInscricaoEquipeIdAndAtletaIdAndStatusIn(
                        inscricaoEquipeId, atletaId, StatusInscricao.OCUPAM_VAGA)
                : repository.existsByInscricaoEquipeIdAndAtletaIdAndIdNotAndStatusIn(
                        inscricaoEquipeId, atletaId, idIgnorado, StatusInscricao.OCUPAM_VAGA);
        if (atletaDuplicado) {
            throw new AtletaJaInscritoException("Atleta já está inscrito nesta equipe");
        }

        boolean atletaEmOutraEquipe = idIgnorado == null
                ? repository.existsByInscricaoEquipeCompeticaoIdAndAtletaIdAndStatusIn(
                        competicaoId, atletaId, StatusInscricao.OCUPAM_VAGA)
                : repository.existsByInscricaoEquipeCompeticaoIdAndAtletaIdAndIdNotAndStatusIn(
                        competicaoId, atletaId, idIgnorado, StatusInscricao.OCUPAM_VAGA);
        if (atletaEmOutraEquipe) {
            throw new AtletaJaInscritoException("Atleta já está inscrito em outra equipe desta competição");
        }

        boolean camisaDuplicada = idIgnorado == null
                ? repository.existsByInscricaoEquipeIdAndNumeroCamisaAndStatusIn(
                        inscricaoEquipeId, numeroCamisa, StatusInscricao.OCUPAM_VAGA)
                : repository.existsByInscricaoEquipeIdAndNumeroCamisaAndIdNotAndStatusIn(
                        inscricaoEquipeId, numeroCamisa, idIgnorado, StatusInscricao.OCUPAM_VAGA);
        if (camisaDuplicada) {
            throw new NumeroCamisaJaUtilizadoException("Número da camisa já utilizado nesta inscrição");
        }
    }

    private void validarRemocaoUltimoAtleta(InscricaoAtleta inscricao, StatusInscricao novoStatus) {
        boolean removeDaAtiva = novoStatus == StatusInscricao.CANCELADA
                || novoStatus == StatusInscricao.INATIVA
                || novoStatus == StatusInscricao.REJEITADA;
        boolean estavaAtiva = inscricao.getStatus() == StatusInscricao.PENDENTE
                || inscricao.getStatus() == StatusInscricao.APROVADA;

        if (!removeDaAtiva || !estavaAtiva) {
            return;
        }

        var inscricaoEquipe = inscricao.getInscricaoEquipe();
        if (inscricaoEquipe == null || inscricaoEquipe.getStatus() != StatusInscricao.APROVADA) {
            return;
        }

        long atletasAtivos = repository.countByInscricaoEquipeIdAndStatusIn(
                inscricaoEquipe.getId(), List.of(StatusInscricao.PENDENTE, StatusInscricao.APROVADA));
        if (atletasAtivos <= 1) {
            throw new UltimoAtletaAtivoException(
                    "Equipe aprovada precisa manter ao menos um atleta ativo");
        }
    }

    private void validarAtletaAtivo(Atleta atleta) {
        if (atleta.getStatus() != StatusAtleta.ATIVO) {
            throw new InscricaoInvalidaException("Atleta inativo não pode ser inscrito");
        }
    }

    private void validarCategoriaAtiva(Categoria categoria) {
        if (categoria.getStatus() != StatusCategoria.ATIVO) {
            throw new InscricaoInvalidaException("Categoria inativa não pode receber inscrições");
        }
    }

    private void validarCompeticaoAberta(Competicao competicao) {
        if (competicao.calcularStatus(LocalDate.now(clock)) != StatusCompeticao.INSCRICOES_ABERTAS) {
            throw new InscricaoInvalidaException("Competição não está com inscrições abertas");
        }
    }

    private void validarInscricaoEquipeAtiva(InscricaoEquipe inscricaoEquipe) {
        if (inscricaoEquipe.getStatus() != StatusInscricao.PENDENTE
                && inscricaoEquipe.getStatus() != StatusInscricao.APROVADA) {
            throw new InscricaoInvalidaException("Inscrição de equipe não está ativa");
        }
    }

    private void validarIdade(Atleta atleta, Categoria categoria) {
        if (atleta.getDataNascimento() == null) return;
        int idade = Period.between(atleta.getDataNascimento(), LocalDate.now(clock)).getYears();
        if (idade < categoria.getIdadeMinima() || idade > categoria.getIdadeMaxima()) {
            throw new AtletaForaDaCategoriaException("Idade do atleta está fora da faixa da categoria");
        }
    }

    private InscricaoEquipe buscarInscricaoEquipe(Long id) {
        return inscricaoEquipeRepository.findById(id)
                .orElseThrow(() -> new InscricaoEquipeNotFoundException("Inscrição de equipe não encontrada"));
    }

    private Atleta buscarAtleta(Long id) {
        return atletaRepository.findById(id)
                .orElseThrow(() -> new AtletaNotFoundException("Atleta não encontrado"));
    }
}
