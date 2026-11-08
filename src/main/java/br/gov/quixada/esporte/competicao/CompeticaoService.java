package br.gov.quixada.esporte.competicao;

import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import br.gov.quixada.esporte.competicao.exception.TransicaoStatusCompeticaoException;
import br.gov.quixada.esporte.inscricao.InscricaoEquipeService;
import br.gov.quixada.esporte.modalidade.Modalidade;
import br.gov.quixada.esporte.partida.PartidaService;
import br.gov.quixada.esporte.modalidade.ModalidadeRepository;
import br.gov.quixada.esporte.modalidade.exception.ModalidadeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CompeticaoService {

    private final CompeticaoRepository repository;
    private final ModalidadeRepository modalidadeRepository;
    private final InscricaoEquipeService inscricaoEquipeService;
    private final PartidaService partidaService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<Competicao> findAll(String nome, StatusCompeticao status, Pageable pageable) {
        if (nome == null || nome.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByNomeContainingIgnoreCase(nome, pageable)
                : repository.findByNomeContainingIgnoreCaseAndStatus(nome, status, pageable);
    }

    @Transactional(readOnly = true)
    public Competicao findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CompeticaoNotFoundException("Competição não encontrada"));
    }

    @Transactional
    public Competicao save(Competicao competicao, Long modalidadeId) {
        competicao.validar();
        competicao.definirModalidade(buscarModalidade(modalidadeId));
        competicao.sincronizarStatus(LocalDate.now(clock));
        return repository.save(competicao);
    }

    @Transactional
    public Competicao alterarStatus(Long id, StatusCompeticao status) {
        if (status != StatusCompeticao.CANCELADA) {
            throw new TransicaoStatusCompeticaoException(
                    "O status da competição é atualizado automaticamente pelas datas; apenas CANCELADA é manual");
        }

        Competicao competicao = findByIdOrThrowNotFound(id);
        if (competicao.getStatus() == StatusCompeticao.CANCELADA) {
            return competicao;
        }

        competicao.cancelar();
        inscricaoEquipeService.cancelarPorCompeticao(id);
        partidaService.cancelarPorCompeticao(id);
        return competicao;
    }

    /**
     * Atualiza dados mutáveis (nome, ano e datas). Modalidade é imutável após a criação;
     * bloqueado quando FINALIZADA ou CANCELADA.
     */
    @Transactional
    public Competicao update(Long id, Competicao competicaoParaAtualizar) {
        Competicao competicaoExistente = findByIdOrThrowNotFound(id);
        competicaoExistente.atualizarDados(
                competicaoParaAtualizar.getNome(),
                competicaoParaAtualizar.getAno(),
                competicaoParaAtualizar.getDataInicioInscricao(),
                competicaoParaAtualizar.getDataFimInscricao(),
                competicaoParaAtualizar.getDataInicio(),
                competicaoParaAtualizar.getDataFim()
        );
        competicaoExistente.sincronizarStatus(LocalDate.now(clock));
        return competicaoExistente;
    }

    private Modalidade buscarModalidade(Long id) {
        return modalidadeRepository.findById(id)
                .orElseThrow(() -> new ModalidadeNotFoundException("Modalidade não encontrada"));
    }
}
