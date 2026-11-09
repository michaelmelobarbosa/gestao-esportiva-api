package br.gov.quixada.esporte.equipe;

import br.gov.quixada.esporte.clube.Clube;
import br.gov.quixada.esporte.clube.ClubeRepository;
import br.gov.quixada.esporte.clube.StatusClube;
import br.gov.quixada.esporte.clube.exception.ClubeInativoException;
import br.gov.quixada.esporte.clube.exception.ClubeNotFoundException;
import br.gov.quixada.esporte.common.error.VinculoAtivoException;
import br.gov.quixada.esporte.equipe.exception.EquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.InscricaoEquipeRepository;
import br.gov.quixada.esporte.inscricao.StatusInscricao;
import br.gov.quixada.esporte.modalidade.StatusModalidade;
import br.gov.quixada.esporte.modalidade.exception.ModalidadeInativaException;
import br.gov.quixada.esporte.modalidade.Modalidade;
import br.gov.quixada.esporte.modalidade.ModalidadeRepository;
import br.gov.quixada.esporte.modalidade.exception.ModalidadeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EquipeService {

    private final EquipeRepository repository;
    private final ClubeRepository clubeRepository;
    private final ModalidadeRepository modalidadeRepository;
    private final InscricaoEquipeRepository inscricaoEquipeRepository;

    @Transactional(readOnly = true)
    public Page<Equipe> findAll(String nome, StatusEquipe status, Pageable pageable) {
        if (nome == null || nome.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByNomeContainingIgnoreCase(nome, pageable)
                : repository.findByNomeContainingIgnoreCaseAndStatus(nome, status, pageable);
    }

    @Transactional(readOnly = true)
    public Equipe findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EquipeNotFoundException("Equipe não encontrada"));
    }

    @Transactional
    public Equipe save(Equipe equipe, Long clubeId, Long modalidadeId) {
        Clube clube = buscarClube(clubeId);
        if (clube.getStatus() != StatusClube.ATIVO) {
            throw new ClubeInativoException("Clube está inativo, não pode ser vinculado a uma equipe");
        }

        var modalidade = buscarModalidade(modalidadeId);
        if (modalidade.getStatus() != StatusModalidade.ATIVO) {
            throw new ModalidadeInativaException("Modalidade está inativa, não pode ser vinculada a uma equipe");
        }

        equipe.ativar();
        equipe.definirClube(clube);
        equipe.definirModalidade(modalidade);
        return repository.save(equipe);
    }

    @Transactional
    public Equipe ativar(Long id) {
        Equipe equipe = findByIdOrThrowNotFound(id);
        equipe.ativar();
        return equipe;
    }

    @Transactional
    public Equipe inativar(Long id) {
        Equipe equipe = findByIdOrThrowNotFound(id);
        if (inscricaoEquipeRepository.existsByEquipeIdAndStatusIn(id, StatusInscricao.OCUPAM_VAGA)) {
            throw new VinculoAtivoException("Equipe possui inscrições ativas e não pode ser inativada");
        }
        equipe.inativar();
        return equipe;
    }

    /**
     * Atualiza dados mutáveis (nome e responsável). Clube e modalidade são imutáveis após a criação.
     */
    @Transactional
    public Equipe update(Long id, Equipe equipeParaAtualizar) {
        Equipe equipeExistente = findByIdOrThrowNotFound(id);
        equipeExistente.atualizarDados(
                equipeParaAtualizar.getNome(),
                equipeParaAtualizar.getResponsavel()
        );
        return equipeExistente;
    }

    private Clube buscarClube(Long id) {
        return clubeRepository.findById(id)
                .orElseThrow(() -> new ClubeNotFoundException("Clube não encontrado"));
    }

    private Modalidade buscarModalidade(Long id) {
        return modalidadeRepository.findById(id)
                .orElseThrow(() -> new ModalidadeNotFoundException("Modalidade não encontrada"));
    }
}
