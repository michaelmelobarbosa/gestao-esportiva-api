package br.gov.quixada.esporte.clube;

import br.gov.quixada.esporte.clube.exception.ClubeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubeService {

    private final ClubeRepository repository;

    @Transactional(readOnly = true)
    public Page<Clube> findAll(String nome, Pageable pageable) {
        return nome == null || nome.isBlank()
                ? repository.findAll(pageable)
                : repository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    @Transactional(readOnly = true)
    public Clube findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ClubeNotFoundException("Clube não encontrado"));
    }

    @Transactional
    public Clube save(Clube clube) {
        clube.ativar();
        return repository.save(clube);
    }

    @Transactional
    public Clube ativar(Long id) {
        Clube clube = findByIdOrThrowNotFound(id);
        clube.ativar();
        return clube;
    }

    @Transactional
    public Clube inativar(Long id) {
        Clube clube = findByIdOrThrowNotFound(id);
        clube.inativar();
        return clube;
    }

    /**
     * Atualiza dados mutáveis. Status (ATIVO/INATIVO) é alterado apenas via soft delete.
     */
    @Transactional
    public Clube update(Long id, Clube clubeParaAtualizar) {
        Clube clubeExistente = findByIdOrThrowNotFound(id);
        clubeExistente.atualizarDados(
                clubeParaAtualizar.getNome(),
                clubeParaAtualizar.getResponsavel(),
                clubeParaAtualizar.getTelefone(),
                clubeParaAtualizar.getEndereco()
        );
        return clubeExistente;
    }
}
