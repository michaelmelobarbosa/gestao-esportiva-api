package br.gov.quixada.esporte.atleta;


import br.gov.quixada.esporte.atleta.dto.AtletaCreateRequest;
import br.gov.quixada.esporte.atleta.dto.AtletaResponse;
import br.gov.quixada.esporte.atleta.dto.AtletaResumoResponse;
import br.gov.quixada.esporte.atleta.dto.AtletaUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/v1/atletas")
@RequiredArgsConstructor
public class AtletaController {

    private final AtletaService service;
    private final AtletaMapper mapper;


    @GetMapping()
    public ResponseEntity<Page<AtletaResumoResponse>> findAll(@RequestParam(required = false) String nome, Pageable pageable) {

        return ResponseEntity.ok(service.findAll(nome, pageable).map(mapper::toResumo));
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

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toGetResponse(saved));
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<AtletaResponse> inativarById(@PathVariable Long id) {

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
