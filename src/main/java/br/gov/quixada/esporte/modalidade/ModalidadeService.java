package br.gov.quixada.esporte.modalidade;

import br.gov.quixada.esporte.common.error.VinculoAtivoException;
import br.gov.quixada.esporte.competicao.CompeticaoRepository;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.equipe.EquipeRepository;
import br.gov.quixada.esporte.equipe.StatusEquipe;
import br.gov.quixada.esporte.modalidade.exception.ModalidadeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModalidadeService {

    private final ModalidadeRepository repository;
    private final EquipeRepository equipeRepository;
    private final CompeticaoRepository competicaoRepository;

    @Transactional(readOnly = true)
    public Page<Modalidade> findAll(String nome, StatusModalidade status, Pageable pageable) {
        if (nome == null || nome.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByNomeContainingIgnoreCase(nome, pageable)
                : repository.findByNomeContainingIgnoreCaseAndStatus(nome, status, pageable);
    }

    @Transactional(readOnly = true)
    public Modalidade findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ModalidadeNotFoundException("Modalidade não encontrada"));
    }

    @Transactional
    public Modalidade save(Modalidade modalidade) {
        modalidade.definirStatus(StatusModalidade.ATIVO);
        return repository.save(modalidade);
    }

    @Transactional
    public Modalidade ativar(Long id) {
        Modalidade modalidade = findByIdOrThrowNotFound(id);
        modalidade.ativar();
        return modalidade;
    }

    @Transactional
    public Modalidade inativar(Long id) {
        Modalidade modalidade = findByIdOrThrowNotFound(id);
        boolean possuiEquipeAtiva = equipeRepository.existsByModalidadeIdAndStatus(id, StatusEquipe.ATIVO);
        boolean possuiCompeticaoAtiva = competicaoRepository.existsByModalidadeIdAndStatusNotIn(
                id, List.of(StatusCompeticao.FINALIZADA, StatusCompeticao.CANCELADA));
        if (possuiEquipeAtiva || possuiCompeticaoAtiva) {
            throw new VinculoAtivoException("Modalidade possui equipes ou competições ativas e não pode ser inativada");
        }
        modalidade.inativar();
        return modalidade;
    }

    /** Atualiza apenas dados mutáveis (nome). Bloqueia edição se INATIVO. */
    @Transactional
    public Modalidade update(Long id, Modalidade modalidadeParaAtualizar) {
        Modalidade modalidadeExistente = findByIdOrThrowNotFound(id);
        modalidadeExistente.atualizarDados(modalidadeParaAtualizar.getNome());
        return modalidadeExistente;
    }
}
