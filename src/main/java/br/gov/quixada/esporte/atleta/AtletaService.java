package br.gov.quixada.esporte.atleta;

import java.sql.SQLIntegrityConstraintViolationException;

import br.gov.quixada.esporte.atleta.exception.AtletaNaoExcluivelException;
import br.gov.quixada.esporte.atleta.exception.AtletaNotFoundException;
import br.gov.quixada.esporte.atleta.exception.CpfJaCadastradoException;
import br.gov.quixada.esporte.common.error.VinculoAtivoException;
import br.gov.quixada.esporte.extras.CpfUtils;
import br.gov.quixada.esporte.extras.NomeCompletoUtils;
import br.gov.quixada.esporte.inscricao.InscricaoAtletaRepository;
import br.gov.quixada.esporte.inscricao.StatusInscricao;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AtletaService {

    private final AtletaRepository repository;
    private final InscricaoAtletaRepository inscricaoAtletaRepository;


    @Transactional(readOnly = true)
    public Page<Atleta> findAll(String nome, StatusAtleta status, Pageable pageable) {
        if (nome == null || nome.isBlank()) {
            return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
        }

        var nomeNormalizado = NomeCompletoUtils.normalize(nome);
        return status == null
                ? repository.findByNomeCompletoNormalizadoStartingWith(nomeNormalizado, pageable)
                : repository.findByNomeCompletoNormalizadoStartingWithAndStatus(nomeNormalizado, status, pageable);
    }

    @Transactional(readOnly = true)
    public Atleta findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AtletaNotFoundException("Atleta não encontrado"));
    }

    @Transactional(readOnly = true)
    public Atleta findByCpfOrThrowNotFound(String cpf) {
        return repository.findByCpf(CpfUtils.normalize(cpf))
                .orElseThrow(() -> new AtletaNotFoundException("Atleta não encontrado"));
    }

    @Transactional
    public Atleta save(Atleta atleta) {
        atleta.definirCpfNormalizado(atleta.getCpf());
        atleta.definirNomeCompletoNormalizado(atleta.getNomeCompleto());
        atleta.definirStatus(StatusAtleta.ATIVO);

        try {
            return repository.save(atleta);
        } catch (DataIntegrityViolationException e) {
            // MySQL error 1062 = duplicate entry (constraint unique do cpf)
            Throwable causa = e.getMostSpecificCause();
            if (causa instanceof SQLIntegrityConstraintViolationException sql && sql.getErrorCode() == 1062) {
                throw new CpfJaCadastradoException("CPF já cadastrado");
            }
            throw e;
        }
    }

    /**
     * Exclusão física do atleta. Só permitida para registro INATIVO e sem
     * inscrições que ainda ocupam vaga; as inscrições que já liberam vaga
     * (CANCELADA/REJEITADA) são removidas junto.
     */
    @Transactional
    public void hardDelete(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);

        if (atleta.getStatus() != StatusAtleta.INATIVO) {
            throw new AtletaNaoExcluivelException("Atleta ativo não pode ser excluído; inative antes de excluir");
        }
        if (inscricaoAtletaRepository.existsByAtletaIdAndStatusIn(id, StatusInscricao.OCUPAM_VAGA)) {
            throw new AtletaNaoExcluivelException(
                    "Atleta possui inscrições ativas e não pode ser excluído");
        }

        inscricaoAtletaRepository.deleteByAtletaIdAndStatusIn(id, StatusInscricao.LIBERAM_VAGA);
        repository.delete(atleta);
    }

    @Transactional
    public Atleta ativar(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
        atleta.ativar();
        return atleta;
    }

    @Transactional
    public Atleta inativar(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
        if (inscricaoAtletaRepository.existsByAtletaIdAndStatusIn(id, StatusInscricao.OCUPAM_VAGA)) {
            throw new VinculoAtivoException("Atleta possui inscrições ativas e não pode ser inativado");
        }
        atleta.inativar();
        return atleta;
    }

    /**
     * CPF imutável após criação; bloqueia edição se INATIVO (delegado à entity).
     */
    @Transactional
    public Atleta update(Long id, Atleta atletaParaAtualizar) {
        Atleta atletaExistente = findByIdOrThrowNotFound(id);
        atletaExistente.atualizarDados(
                atletaParaAtualizar.getNomeCompleto(),
                atletaParaAtualizar.getDataNascimento(),
                atletaParaAtualizar.getEndereco(),
                atletaParaAtualizar.getTelefone(),
                atletaParaAtualizar.getSexo()
        );
        return atletaExistente;
    }
}
