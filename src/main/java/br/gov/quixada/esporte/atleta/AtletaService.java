package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.exceptions.AtletaNotFoundException;
import br.gov.quixada.esporte.exceptions.CpfJaCadastradoException;
import br.gov.quixada.esporte.extras.StatusAtleta;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AtletaService {

    private final AtletaRepository repository;

    @Transactional(readOnly = true)
    public List<Atleta> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Atleta findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AtletaNotFoundException(HttpStatus.NOT_FOUND, "Atleta não encontrado"));
    }

    @Transactional(readOnly = true)
    public Atleta findByCpfOrThrowNotFound(String cpf) {
        return repository.findByCpf(cpf)
                .orElseThrow(() -> new AtletaNotFoundException(HttpStatus.NOT_FOUND, "Atleta não encontrado"));
    }

    @Transactional
    public Atleta save(Atleta atleta) {
        if (repository.existsByCpf(atleta.getCpf())) {
            throw new CpfJaCadastradoException(HttpStatus.CONFLICT, "CPF já cadastrado");
        }

        atleta.setStatus(StatusAtleta.ATIVO);
        return repository.save(atleta);

    }

    @Transactional
    public void delete(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
        repository.delete(atleta);
    }

    @Transactional
    public void ativar(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
        atleta.setStatus(StatusAtleta.ATIVO);
    }

    @Transactional
    public void inativar(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
        atleta.setStatus(StatusAtleta.INATIVO);
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
