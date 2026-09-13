package br.gov.quixada.esporte.competicao;

import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
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
public class CompeticaoService {

    private final CompeticaoRepository repository;
    private final ModalidadeRepository modalidadeRepository;

    @Transactional(readOnly = true)
    public Page<Competicao> findAll(String nome, Pageable pageable) {
        return nome == null || nome.isBlank()
                ? repository.findAll(pageable)
                : repository.findByNomeContainingIgnoreCase(nome, pageable);
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
        return repository.save(competicao);
    }

    @Transactional
    public Competicao alterarStatus(Long id, StatusCompeticao status) {
        Competicao competicao = findByIdOrThrowNotFound(id);
        competicao.alterarStatus(status);
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
                competicaoParaAtualizar.getDataInicio(),
                competicaoParaAtualizar.getDataFim()
        );
        return competicaoExistente;
    }

    private Modalidade buscarModalidade(Long id) {
        return modalidadeRepository.findById(id)
                .orElseThrow(() -> new ModalidadeNotFoundException("Modalidade não encontrada"));
    }
}
