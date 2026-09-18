package br.gov.quixada.esporte.atleta;

import java.sql.SQLIntegrityConstraintViolationException;

import br.gov.quixada.esporte.atleta.exception.AtletaNotFoundException;
import br.gov.quixada.esporte.atleta.exception.CpfJaCadastradoException;
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


    @Transactional(readOnly = true)
    public Page<Atleta> findAll(String nome, Pageable pageable) {
        return nome == null || nome.isBlank() ? repository.findAll(pageable) :
                repository.findByNomeCompletoContainingIgnoreCase(nome, pageable);
    }

    @Transactional(readOnly = true)
    public Atleta findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AtletaNotFoundException("Atleta não encontrado"));
    }

//    @Transactional(readOnly = true)
//    public Atleta findByCpfOrThrowNotFound(String cpf) {
//        return repository.findByCpf(CpfUtils.normalize(cpf))
//                .orElseThrow(() -> new AtletaNotFoundException("Atleta não encontrado"));
//    }

    @Transactional
    public Atleta save(Atleta atleta) {
        atleta.definirCpfNormalizado(atleta.getCpf());
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

    // hard delete a ser implementado quando perfil admin for criado
    // @Transactional
    // public void hardDelete(Long id) {
    // Atleta atleta = findByIdOrThrowNotFound(id);
    // repository.delete(atleta);
    // }

    @Transactional
    public Atleta ativar(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
        atleta.ativar();
        return atleta;
    }

    @Transactional
    public Atleta inativar(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
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
