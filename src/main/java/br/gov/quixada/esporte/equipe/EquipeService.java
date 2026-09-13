package br.gov.quixada.esporte.equipe;

import br.gov.quixada.esporte.clube.Clube;
import br.gov.quixada.esporte.clube.ClubeRepository;
import br.gov.quixada.esporte.clube.exception.ClubeNotFoundException;
import br.gov.quixada.esporte.equipe.exception.EquipeNotFoundException;
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

    @Transactional(readOnly = true)
    public Page<Equipe> findAll(String nome, Pageable pageable) {
        return nome == null || nome.isBlank()
                ? repository.findAll(pageable)
                : repository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    @Transactional(readOnly = true)
    public Equipe findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EquipeNotFoundException("Equipe não encontrada"));
    }

    @Transactional
    public Equipe save(Equipe equipe, Long clubeId, Long modalidadeId) {
        equipe.ativar();
        equipe.definirClube(buscarClube(clubeId));
        equipe.definirModalidade(buscarModalidade(modalidadeId));
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
