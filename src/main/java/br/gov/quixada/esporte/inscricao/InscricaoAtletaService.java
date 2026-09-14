package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.atleta.Atleta;
import br.gov.quixada.esporte.atleta.AtletaRepository;
import br.gov.quixada.esporte.atleta.exception.AtletaNotFoundException;
import br.gov.quixada.esporte.categoria.Categoria;
import br.gov.quixada.esporte.inscricao.exception.AtletaForaDaCategoriaException;
import br.gov.quixada.esporte.inscricao.exception.AtletaJaInscritoException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoAtletaNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.NumeroCamisaJaUtilizadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class InscricaoAtletaService {

    private final InscricaoAtletaRepository repository;
    private final InscricaoEquipeRepository inscricaoEquipeRepository;
    private final AtletaRepository atletaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<InscricaoAtleta> findAll(Long inscricaoEquipeId, Pageable pageable) {
        return inscricaoEquipeId == null
                ? repository.findAll(pageable)
                : repository.findByInscricaoEquipeId(inscricaoEquipeId, pageable);
    }

    @Transactional(readOnly = true)
    public InscricaoAtleta findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new InscricaoAtletaNotFoundException("Inscrição de atleta não encontrada"));
    }

    @Transactional
    public InscricaoAtleta save(InscricaoAtleta inscricao, Long inscricaoEquipeId, Long atletaId) {
        InscricaoEquipe inscricaoEquipe = buscarInscricaoEquipe(inscricaoEquipeId);
        Atleta atleta = buscarAtleta(atletaId);

        if (repository.existsByInscricaoEquipeIdAndAtletaId(inscricaoEquipeId, atletaId)) {
            throw new AtletaJaInscritoException("Atleta já está inscrito nesta equipe");
        }
        if (repository.existsByInscricaoEquipeCompeticaoIdAndAtletaId(inscricaoEquipe.getCompeticao().getId(), atletaId)) {
            throw new AtletaJaInscritoException("Atleta já está inscrito em outra equipe desta competição");
        }
        if (repository.existsByInscricaoEquipeIdAndNumeroCamisa(inscricaoEquipeId, inscricao.getNumeroCamisa())) {
            throw new NumeroCamisaJaUtilizadoException("Número da camisa já utilizado nesta inscrição");
        }
        validarIdade(atleta, inscricaoEquipe.getCategoria());

        inscricao.definirInscricaoEquipe(inscricaoEquipe);
        inscricao.definirAtleta(atleta);
        inscricao.definirStatus(StatusInscricao.PENDENTE);
        return repository.save(inscricao);
    }

    @Transactional
    public InscricaoAtleta alterarStatus(Long id, StatusInscricao status) {
        InscricaoAtleta inscricao = findByIdOrThrowNotFound(id);
        inscricao.alterarStatus(status);
        return inscricao;
    }

    private void validarIdade(Atleta atleta, Categoria categoria) {
        if (atleta.getDataNascimento() == null) return;
        int idade = Period.between(atleta.getDataNascimento(), LocalDate.now(clock)).getYears();
        if (idade < categoria.getIdadeMinima() || idade > categoria.getIdadeMaxima()) {
            throw new AtletaForaDaCategoriaException("Idade do atleta está fora da faixa da categoria");
        }
    }

    private InscricaoEquipe buscarInscricaoEquipe(Long id) {
        return inscricaoEquipeRepository.findById(id)
                .orElseThrow(() -> new InscricaoEquipeNotFoundException("Inscrição de equipe não encontrada"));
    }

    private Atleta buscarAtleta(Long id) {
        return atletaRepository.findById(id)
                .orElseThrow(() -> new AtletaNotFoundException("Atleta não encontrado"));
    }
}
