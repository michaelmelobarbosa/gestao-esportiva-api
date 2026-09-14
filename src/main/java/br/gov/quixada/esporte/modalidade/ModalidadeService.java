package br.gov.quixada.esporte.modalidade;

import br.gov.quixada.esporte.modalidade.exception.ModalidadeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModalidadeService {

    private final ModalidadeRepository repository;

    @Transactional(readOnly = true)
    public Page<Modalidade> findAll(String nome, Pageable pageable) {
        return nome == null || nome.isBlank()
                ? repository.findAll(pageable)
                : repository.findByNomeContainingIgnoreCase(nome, pageable);
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
