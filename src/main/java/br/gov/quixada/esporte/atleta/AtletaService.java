package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.exceptions.AtletaInativoException;
import br.gov.quixada.esporte.exceptions.AtletaNotFoundException;
import br.gov.quixada.esporte.exceptions.CpfJaCadastradoException;
import br.gov.quixada.esporte.extras.CpfUtils;
import br.gov.quixada.esporte.extras.StatusAtleta;
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
        return nome == null || nome.isBlank() ? repository.findAll(pageable) : repository.findByNomeCompletoContainingIgnoreCase(nome, pageable);
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
        atleta.setCpf(CpfUtils.normalize(atleta.getCpf()));
        atleta.setStatus(StatusAtleta.ATIVO);

        try {
            return repository.save(atleta);
        } catch (DataIntegrityViolationException e) {
            throw new CpfJaCadastradoException("CPF já cadastrado");
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
        if (atleta.getStatus() == StatusAtleta.ATIVO) {
            return atleta;
        }

        atleta.setStatus(StatusAtleta.ATIVO);

        return atleta;
    }

    @Transactional
    public Atleta inativar(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);

        if (atleta.getStatus() == StatusAtleta.INATIVO) {
            return atleta;
        }

        atleta.setStatus(StatusAtleta.INATIVO);

        return atleta;
    }

    /**
     * CPF imutável após criação; bloqueia edição se INATIVO.
     */
    @Transactional
    public Atleta update(Long id, Atleta atletaParaAtualizar) {
        Atleta atletaExistente = findByIdOrThrowNotFound(id);

        if (atletaExistente.getStatus() == StatusAtleta.INATIVO) {
            throw new AtletaInativoException("Atleta está inativo, reative antes de editar");
        }

        atletaExistente.setNomeCompleto(atletaParaAtualizar.getNomeCompleto());
        atletaExistente.setTelefone(atletaParaAtualizar.getTelefone());
        atletaExistente.setDataNascimento(atletaParaAtualizar.getDataNascimento());
        atletaExistente.setEndereco(atletaParaAtualizar.getEndereco());
        atletaExistente.setSexo(atletaParaAtualizar.getSexo());

        return atletaExistente;
    }
}
