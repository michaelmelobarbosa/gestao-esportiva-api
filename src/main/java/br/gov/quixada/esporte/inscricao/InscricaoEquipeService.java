package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.categoria.CategoriaRepository;
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

        inscricao.alterarStatus(status);

        if (status == StatusInscricao.CANCELADA || status == StatusInscricao.INATIVA) {
            cascatearStatusAtletas(inscricao.getId(), status);
            partidaService.cancelarPorInscricao(inscricao.getId());
        }

        return inscricao;
    }

    @Transactional
    public void cancelarPorCompeticao(Long competicaoId) {
        List<InscricaoEquipe> inscricoes = repository.findByCompeticaoId(competicaoId);
        for (InscricaoEquipe inscricao : inscricoes) {
            if (inscricao.getStatus() == StatusInscricao.CANCELADA) {
                continue;
            }
            inscricao.alterarStatus(StatusInscricao.CANCELADA);
            cascatearStatusAtletas(inscricao.getId(), StatusInscricao.CANCELADA);
        }
    }

    private void cascatearStatusAtletas(Long inscricaoEquipeId, StatusInscricao status) {
        for (InscricaoAtleta inscricaoAtleta : inscricaoAtletaRepository.findByInscricaoEquipeId(inscricaoEquipeId)) {
            if (inscricaoAtleta.getStatus() == StatusInscricao.CANCELADA
                    || inscricaoAtleta.getStatus() == status) {
                continue;
            }
            inscricaoAtleta.alterarStatus(status);
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
