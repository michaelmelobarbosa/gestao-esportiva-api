package br.gov.quixada.esporte.categoria;

import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.CompeticaoRepository;
import br.gov.quixada.esporte.categoria.exception.CategoriaNotFoundException;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repository;
    private final CompeticaoRepository competicaoRepository;

    @Transactional(readOnly = true)
    public Page<Categoria> findAll(String nome, Pageable pageable) {
        return nome == null || nome.isBlank()
                ? repository.findAll(pageable)
                : repository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    @Transactional(readOnly = true)
    public Categoria findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CategoriaNotFoundException("Categoria não encontrada"));
    }

    @Transactional
    public Categoria save(Categoria categoria, Long competicaoId) {
        categoria.validar();
        categoria.ativar();
        categoria.definirCompeticao(buscarCompeticao(competicaoId));
        return repository.save(categoria);
    }

    @Transactional
    public Categoria ativar(Long id) {
        Categoria categoria = findByIdOrThrowNotFound(id);
        categoria.ativar();
        return categoria;
    }

    @Transactional
    public Categoria inativar(Long id) {
        Categoria categoria = findByIdOrThrowNotFound(id);
        categoria.inativar();
        return categoria;
    }

    /**
     * Atualiza apenas dados mutáveis (nome e idades). Competição é imutável após a criação.
     */
    @Transactional
    public Categoria update(Long id, Categoria categoriaParaAtualizar) {
        Categoria categoriaExistente = findByIdOrThrowNotFound(id);
        categoriaExistente.atualizarDados(
                categoriaParaAtualizar.getNome(),
                categoriaParaAtualizar.getIdadeMinima(),
                categoriaParaAtualizar.getIdadeMaxima()
        );
        return categoriaExistente;
    }

    private Competicao buscarCompeticao(Long id) {
        return competicaoRepository.findById(id)
                .orElseThrow(() -> new CompeticaoNotFoundException("Competição não encontrada"));
    }
}
