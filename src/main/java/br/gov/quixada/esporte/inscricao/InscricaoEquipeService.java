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
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoInvalidaException;
import br.gov.quixada.esporte.inscricao.exception.InscricoesEncerradasException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InscricaoEquipeService {

    private final InscricaoEquipeRepository repository;
    private final CompeticaoRepository competicaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final EquipeRepository equipeRepository;

    @Transactional(readOnly = true)
    public Page<InscricaoEquipe> findAll(Long competicaoId, Pageable pageable) {
        return competicaoId == null
                ? repository.findAll(pageable)
                : repository.findByCompeticaoId(competicaoId, pageable);
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

        if (competicao.getStatus() != StatusCompeticao.INSCRICOES_ABERTAS) {
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

        inscricao.definirCompeticao(competicao);
        inscricao.definirCategoria(categoria);
        inscricao.definirEquipe(equipe);
        inscricao.definirStatus(StatusInscricao.PENDENTE);
        return repository.save(inscricao);
    }

    @Transactional
    public InscricaoEquipe alterarStatus(Long id, StatusInscricao status) {
        InscricaoEquipe inscricao = findByIdOrThrowNotFound(id);
        inscricao.alterarStatus(status);
        return inscricao;
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
