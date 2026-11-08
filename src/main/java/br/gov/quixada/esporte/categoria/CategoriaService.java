package br.gov.quixada.esporte.categoria;

import br.gov.quixada.esporte.categoria.exception.CategoriaDuplicadaException;
import br.gov.quixada.esporte.categoria.exception.CategoriaNotFoundException;
import br.gov.quixada.esporte.common.error.VinculoAtivoException;
import br.gov.quixada.esporte.competicao.Competicao;
import br.gov.quixada.esporte.competicao.CompeticaoRepository;
import br.gov.quixada.esporte.competicao.StatusCompeticao;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNaoEditavelException;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import br.gov.quixada.esporte.inscricao.InscricaoEquipeRepository;
import br.gov.quixada.esporte.inscricao.StatusInscricao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repository;
    private final CompeticaoRepository competicaoRepository;
    private final InscricaoEquipeRepository inscricaoEquipeRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<Categoria> findAll(String nome, StatusCategoria status, Pageable pageable) {
        if (nome == null || nome.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }
        return status == null
                ? repository.findByNomeContainingIgnoreCase(nome, pageable)
                : repository.findByNomeContainingIgnoreCaseAndStatus(nome, status, pageable);
    }

    @Transactional(readOnly = true)
    public Categoria findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CategoriaNotFoundException("Categoria não encontrada"));
    }

    @Transactional
    public Categoria save(Categoria categoria, Long competicaoId) {
        categoria.validar();
        Competicao competicao = buscarCompeticao(competicaoId);
        validarCompeticaoEditavel(competicao);
        validarFaixaDuplicada(competicaoId, categoria.getIdadeMinima(), categoria.getIdadeMaxima(), null);

        categoria.ativar();
        categoria.definirCompeticao(competicao);
        return repository.save(categoria);
    }

    @Transactional
    public Categoria ativar(Long id) {
        Categoria categoria = findByIdOrThrowNotFound(id);
        validarCompeticaoEditavel(categoria.getCompeticao());
        validarFaixaDuplicada(competicaoId(categoria), categoria.getIdadeMinima(),
                categoria.getIdadeMaxima(), categoria.getId());
        categoria.ativar();
        return categoria;
    }

    @Transactional
    public Categoria inativar(Long id) {
        Categoria categoria = findByIdOrThrowNotFound(id);
        if (inscricaoEquipeRepository.existsByCategoriaIdAndStatusIn(id, StatusInscricao.OCUPAM_VAGA)) {
            throw new VinculoAtivoException("Categoria possui inscrições ativas e não pode ser inativada");
        }
        categoria.inativar();
        return categoria;
    }

    /**
     * Atualiza apenas dados mutáveis (nome e idades). Competição é imutável após a criação.
     */
    @Transactional
    public Categoria update(Long id, Categoria categoriaParaAtualizar) {
        Categoria categoriaExistente = findByIdOrThrowNotFound(id);
        validarCompeticaoEditavel(categoriaExistente.getCompeticao());
        validarFaixaDuplicada(competicaoId(categoriaExistente),
                categoriaParaAtualizar.getIdadeMinima(), categoriaParaAtualizar.getIdadeMaxima(), id);
        categoriaExistente.atualizarDados(
                categoriaParaAtualizar.getNome(),
                categoriaParaAtualizar.getIdadeMinima(),
                categoriaParaAtualizar.getIdadeMaxima()
        );
        return categoriaExistente;
    }

    private void validarFaixaDuplicada(Long competicaoId, Integer idadeMinima, Integer idadeMaxima, Long idIgnorado) {
        if (competicaoId == null) {
            return;
        }
        boolean duplicada = idIgnorado == null
                ? repository.existsByCompeticaoIdAndIdadeMinimaAndIdadeMaximaAndStatus(
                        competicaoId, idadeMinima, idadeMaxima, StatusCategoria.ATIVO)
                : repository.existsByCompeticaoIdAndIdadeMinimaAndIdadeMaximaAndStatusAndIdNot(
                        competicaoId, idadeMinima, idadeMaxima, StatusCategoria.ATIVO, idIgnorado);
        if (duplicada) {
            throw new CategoriaDuplicadaException("Já existe uma categoria ativa com esta faixa etária nesta competição");
        }
    }

    private void validarCompeticaoEditavel(Competicao competicao) {
        if (competicao == null) {
            return;
        }
        StatusCompeticao status = competicao.calcularStatus(LocalDate.now(clock));
        if (status == StatusCompeticao.CANCELADA || status == StatusCompeticao.FINALIZADA) {
            throw new CompeticaoNaoEditavelException(
                    "Competição cancelada ou finalizada não permite alterar categorias");
        }
    }

    private Long competicaoId(Categoria categoria) {
        return categoria.getCompeticao() != null ? categoria.getCompeticao().getId() : null;
    }

    private Competicao buscarCompeticao(Long id) {
        return competicaoRepository.findById(id)
                .orElseThrow(() -> new CompeticaoNotFoundException("Competição não encontrada"));
    }
}
