package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.exceptions.AtletaNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.internal.constraintvalidators.hv.br.CPFValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.management.InstanceAlreadyExistsException;

@Service
@RequiredArgsConstructor
public class AtletaService {

    private final AtletaRepository repository;

    public Atleta findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AtletaNotFoundException(HttpStatus.NOT_FOUND, "Atleta não encontrado"));
    }

    public Atleta save(Atleta atleta) {
        if(atleta.getCpf() == null || atleta.getCpf().isEmpty()){
            throw new IllegalArgumentException("CPF é obrigatório");
        }

        return repository.save(atleta);
    }

    public void delete(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
        repository.delete(atleta);
    }

    public void update(Long id, Atleta atletaParaAtualizar) {
        Atleta atletaExistente = findByIdOrThrowNotFound(id);

        atletaExistente.setNomeCompleto(atletaParaAtualizar.getNomeCompleto());
        atletaExistente.setTelefone(atletaParaAtualizar.getTelefone());
        atletaExistente.setDataNascimento(atletaParaAtualizar.getDataNascimento());
        atletaExistente.setEndereco(atletaParaAtualizar.getEndereco());
        atletaExistente.setAtivo(atletaParaAtualizar.isAtivo());
        atletaExistente.setSexo(atletaParaAtualizar.getSexo());

        repository.save(atletaExistente);
    }

    private void assertAtletaExist(Atleta atleta) {
        findByIdOrThrowNotFound(atleta.getId());
    }
}
