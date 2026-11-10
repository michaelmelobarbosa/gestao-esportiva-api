package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.categoria.CategoriaRepository;
import br.gov.quixada.esporte.categoria.StatusCategoria;
import br.gov.quixada.esporte.categoria.exception.CategoriaInativaException;
import br.gov.quixada.esporte.categoria.exception.CategoriaNotFoundException;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.CompeticaoRepository;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import br.gov.quixada.esporte.equipe.Equipe;
import br.gov.quixada.esporte.equipe.EquipeRepository;
import br.gov.quixada.esporte.equipe.StatusEquipe;
import br.gov.quixada.esporte.equipe.exception.EquipeInativaException;
import br.gov.quixada.esporte.equipe.exception.EquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.EquipeJaInscritaException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeSemAtletasException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoInvalidaException;
import br.gov.quixada.esporte.inscricao.exception.InscricoesEncerradasException;
import br.gov.quixada.esporte.inscricao.exception.TransicaoStatusInvalidaException;
import br.gov.quixada.esporte.partida.PartidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InscricaoEquipeService {

    private final InscricaoEquipeRepository repository;
    private final InscricaoAtletaRepository inscricaoAtletaRepository;
    private final CompeticaoRepository competicaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final EquipeRepository equipeRepository;
    private final PartidaService partidaService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<InscricaoEquipe> findAll(Long competicaoId, StatusInscricao status, Pageable pageable) {
        if (competicaoId == null) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByCompeticaoId(competicaoId, pageable)
                : repository.findByCompeticaoIdAndStatus(competicaoId, status, pageable);
    }

    @Transactional(readOnly = true)
    public InscricaoEquipe findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new InscricaoEquipeNotFoundException("Inscrição de equipe não encontrada"));
    }

    @Transactional
    public InscricaoEquipe save(InscricaoEquipe inscricao, Long competicaoId, Long categoriaId, Long equipeId) {
        Competicao competicao = buscarCompeticao(competicaoId);
        Categoria categoria = buscarCategoria(categoriaId);
        Equipe equipe = buscarEquipe(equipeId);

        if (competicao.calcularStatus(LocalDate.now(clock)) != StatusCompeticao.INSCRICOES_ABERTAS) {
            throw new InscricoesEncerradasException("Competição não está com inscrições abertas");
        }
        if (equipe.getStatus() != StatusEquipe.ATIVO) {
            throw new EquipeInativaException("Equipe está inativa, não pode ser inscrita");
        }
        if (categoria.getStatus() != StatusCategoria.ATIVO) {
            throw new CategoriaInativaException("Categoria está inativa, não pode receber inscrições");
        }
        if (!categoria.getCompeticao().getId().equals(competicao.getId())) {
            throw new InscricaoInvalidaException("Categoria não pertence à competição informada");
        }
        if (!equipe.getModalidade().getId().equals(competicao.getModalidade().getId())) {
            throw new InscricaoInvalidaException("Modalidade da equipe difere da modalidade da competição");
        }
        if (repository.existsByCompeticaoIdAndEquipeIdAndStatusIn(
                competicaoId, equipeId, StatusInscricao.OCUPAM_VAGA)) {
            throw new EquipeJaInscritaException("Equipe já possui inscrição ativa nesta competição");
        }

        inscricao.definirCompeticao(competicao);
        inscricao.definirCategoria(categoria);
        inscricao.definirEquipe(equipe);
        inscricao.definirStatus(StatusInscricao.PENDENTE);
        return repository.save(inscricao);
    }

    @Transactional
    public InscricaoEquipe alterarStatus(Long id, StatusInscricao status) {
        InscricaoEquipe inscricao = findByIdOrThrowNotFound(id);

        if (inscricao.getStatus() == status) {
            return inscricao;
        }
        if (!inscricao.getStatus().podeTransicionarPara(status)) {
            throw new TransicaoStatusInvalidaException(
                    "Transição de status inválida: " + inscricao.getStatus() + " -> " + status);
        }

        if (status == StatusInscricao.PENDENTE) {
            revalidarReativacao(inscricao);
        }
        if (status == StatusInscricao.APROVADA) {
            validarMinimoAtletas(inscricao);
        }

        inscricao.alterarStatus(status, LocalDateTime.now(clock));

        if (status == StatusInscricao.CANCELADA || status == StatusInscricao.INATIVA) {
            cascatearStatusAtletas(inscricao.getId(), status);
            partidaService.cancelarPorInscricao(inscricao.getId());
        }

        return inscricao;
    }

    /**
     * Campo livre de comunicação: pode ser criado, alterado ou limpo em qualquer
     * status, sem checagens de negócio.
     */
    @Transactional
    public InscricaoEquipe atualizarObservacao(Long id, String observacao) {
        InscricaoEquipe inscricao = findByIdOrThrowNotFound(id);
        inscricao.atualizarObservacao(observacao);
        return inscricao;
    }

    @Transactional
    public void cancelarPorCompeticao(Long competicaoId) {
        List<InscricaoEquipe> inscricoes = repository.findByCompeticaoId(competicaoId);
        for (InscricaoEquipe inscricao : inscricoes) {
            if (inscricao.getStatus() == StatusInscricao.CANCELADA) {
                continue;
            }
            inscricao.alterarStatus(StatusInscricao.CANCELADA, LocalDateTime.now(clock));
            cascatearStatusAtletas(inscricao.getId(), StatusInscricao.CANCELADA);
        }
    }

    private void revalidarReativacao(InscricaoEquipe inscricao) {
        Competicao competicao = inscricao.getCompeticao();
        Categoria categoria = inscricao.getCategoria();
        Equipe equipe = inscricao.getEquipe();

        if (competicao.calcularStatus(LocalDate.now(clock)) != StatusCompeticao.INSCRICOES_ABERTAS) {
            throw new InscricoesEncerradasException("Competição não está com inscrições abertas");
        }
        if (equipe.getStatus() != StatusEquipe.ATIVO) {
            throw new EquipeInativaException("Equipe está inativa, não pode ser reativada");
        }
        if (categoria.getStatus() != StatusCategoria.ATIVO) {
            throw new CategoriaInativaException("Categoria está inativa, não pode receber inscrições");
        }
        if (!categoria.getCompeticao().getId().equals(competicao.getId())) {
            throw new InscricaoInvalidaException("Categoria não pertence à competição informada");
        }
        if (!equipe.getModalidade().getId().equals(competicao.getModalidade().getId())) {
            throw new InscricaoInvalidaException("Modalidade da equipe difere da modalidade da competição");
        }
        if (repository.existsByCompeticaoIdAndEquipeIdAndStatusInAndIdNot(
                competicao.getId(), equipe.getId(), StatusInscricao.OCUPAM_VAGA, inscricao.getId())) {
            throw new EquipeJaInscritaException("Equipe já possui inscrição ativa nesta competição");
        }
    }

    private void validarMinimoAtletas(InscricaoEquipe inscricao) {
        long atletasAtivos = inscricaoAtletaRepository.countByInscricaoEquipeIdAndStatusIn(
                inscricao.getId(), List.of(StatusInscricao.PENDENTE, StatusInscricao.APROVADA));
        if (atletasAtivos < 1) {
            throw new InscricaoEquipeSemAtletasException(
                    "Inscrição de equipe precisa de ao menos um atleta para ser aprovada");
        }
    }

    private void cascatearStatusAtletas(Long inscricaoEquipeId, StatusInscricao status) {
        for (InscricaoAtleta inscricaoAtleta : inscricaoAtletaRepository.findByInscricaoEquipeId(inscricaoEquipeId)) {
            StatusInscricao atual = inscricaoAtleta.getStatus();

            if (atual == StatusInscricao.CANCELADA || atual == StatusInscricao.REJEITADA || atual == status) {
                continue;
            }
            if (status == StatusInscricao.INATIVA && atual != StatusInscricao.PENDENTE
                    && atual != StatusInscricao.APROVADA) {
                continue;
            }

            inscricaoAtleta.alterarStatus(status, LocalDateTime.now(clock));
        }
    }

    private Competicao buscarCompeticao(Long id) {
        return competicaoRepository.findById(id)
                .orElseThrow(() -> new CompeticaoNotFoundException("Competição não encontrada"));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new CategoriaNotFoundException("Categoria não encontrada"));
    }

    private Equipe buscarEquipe(Long id) {
        return equipeRepository.findById(id)
                .orElseThrow(() -> new EquipeNotFoundException("Equipe não encontrada"));
    }
}
