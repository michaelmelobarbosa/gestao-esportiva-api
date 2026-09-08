package br.gov.quixada.esporte.atleta;


import br.gov.quixada.esporte.atleta.dto.AtletaCreateRequest;
import br.gov.quixada.esporte.atleta.dto.AtletaResponse;
import br.gov.quixada.esporte.atleta.dto.AtletaResumoResponse;
import br.gov.quixada.esporte.atleta.dto.AtletaUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/atletas")
@RequiredArgsConstructor
public class AtletaController {

    private final AtletaService service;
    private final AtletaMapper mapper;

    @GetMapping
    public ResponseEntity<List<AtletaResumoResponse>> findAll(@RequestParam(required = false) String nome) {
        List<Atleta> atletas = service.findAll(nome);
        List<AtletaResumoResponse> resumoList = mapper.toResumoList(atletas);

        return ResponseEntity.ok(resumoList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtletaResponse> findById(@PathVariable Long id) {
        Atleta atleta = service.findByIdOrThrowNotFound(id);
        AtletaResponse response = mapper.toGetResponse(atleta);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AtletaResponse> save(@RequestBody @Valid AtletaCreateRequest request) {
        Atleta atleta = mapper.toEntity(request);
        Atleta saved = service.save(atleta);
        AtletaResponse getResponse = mapper.toGetResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(getResponse);
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<AtletaResponse> desativarById(@PathVariable Long id) {

        Atleta inativo = service.inativar(id);

        return ResponseEntity.ok(mapper.toGetResponse(inativo));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<AtletaResponse> ativarById(@PathVariable Long id) {

        Atleta ativo = service.ativar(id);

        return ResponseEntity.ok(mapper.toGetResponse(ativo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtletaResponse> update(@PathVariable Long id,
                                                 @RequestBody @Valid AtletaUpdateRequest request) {

        Atleta atleta = mapper.toEntity(request);
        Atleta updated = service.update(id, atleta);
        AtletaResponse getResponse = mapper.toGetResponse(updated);

        return ResponseEntity.ok(getResponse);
    }
}
