package br.gov.quixada.esporte.atleta;

import br.gov.quixada.esporte.exceptions.AtletaNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AtletaService {

    private final AtletaRepository repository;

    public Atleta findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AtletaNotFoundException(HttpStatus.NOT_FOUND, "Atleta não encontrado"));
    }

    public Atleta save(Atleta atleta) {
        return repository.save(atleta);
    }

    public void delete(Long id) {
        Atleta atleta = findByIdOrThrowNotFound(id);
        repository.delete(atleta);
    }

    public void update(Atleta atletaToUpdate) {
        assertAtletaExist(atletaToUpdate);
        repository.save(atletaToUpdate);
    }

    public void assertAtletaExist(Atleta atleta) {
        findByIdOrThrowNotFound(atleta.getId());
    }

}
