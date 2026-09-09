package br.gov.quixada.esporte.atleta;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.gov.quixada.esporte.exceptions.AtletaNotFoundException;
import br.gov.quixada.esporte.exceptions.CpfJaCadastradoException;
import br.gov.quixada.esporte.extras.CpfUtils;
import br.gov.quixada.esporte.extras.StatusAtleta;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtletaService {

    private final AtletaRepository repository;

    @Transactional(readOnly = true)
    public List<Atleta> findAll(String name) {
        return name == null ? repository.findAll() : repository.findByNomeCompletoContaining(name);
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

        if (repository.existsByCpf(atleta.getCpf())) {
            throw new CpfJaCadastradoException("CPF já cadastrado");
        }

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
        atleta.setStatus(StatusAtleta.ATIVO);

        return repository.save(atleta);
    }

    @Transactional
    public Atleta inativar(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
        atleta.setStatus(StatusAtleta.INATIVO);
        return repository.save(atleta);
    }

    @Transactional
    public Atleta update(Long id, Atleta atletaParaAtualizar) {
        Atleta atletaExistente = findByIdOrThrowNotFound(id);

        atletaExistente.setNomeCompleto(atletaParaAtualizar.getNomeCompleto());
        atletaExistente.setTelefone(atletaParaAtualizar.getTelefone());
        atletaExistente.setDataNascimento(atletaParaAtualizar.getDataNascimento());
        atletaExistente.setEndereco(atletaParaAtualizar.getEndereco());
        atletaExistente.setSexo(atletaParaAtualizar.getSexo());

        return repository.save(atletaExistente);
    }
}
