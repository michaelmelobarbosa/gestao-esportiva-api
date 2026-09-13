package br.gov.quixada.esporte.clube;

import br.gov.quixada.esporte.clube.dto.ClubeCreateRequest;
import br.gov.quixada.esporte.clube.dto.ClubeResponse;
import br.gov.quixada.esporte.clube.dto.ClubeResumoResponse;
import br.gov.quixada.esporte.clube.dto.ClubeStatusRequest;
import br.gov.quixada.esporte.clube.dto.ClubeUpdateRequest;
import br.gov.quixada.esporte.extras.StatusClube;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/v1/clubes")
@RequiredArgsConstructor
@Validated
public class ClubeController {

    private final ClubeService service;
    private final ClubeMapper mapper;

    @GetMapping
    public ResponseEntity<Page<ClubeResumoResponse>> findAll(@RequestParam(required = false) String nome, Pageable pageable) {
        return ResponseEntity.ok(service.findAll(nome, pageable).map(mapper::toResumo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClubeResponse> findById(@PathVariable @Positive Long id) {
        Clube clube = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(clube));
    }

    @PostMapping
    public ResponseEntity<ClubeResponse> save(@RequestBody @Valid ClubeCreateRequest request) {
        Clube clube = mapper.toEntity(request);
        Clube saved = service.save(clube);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ClubeResponse> alterarStatus(@PathVariable @Positive Long id,
                                                       @RequestBody @Valid ClubeStatusRequest request) {
        Clube clube = request.status() == StatusClube.ATIVO ? service.ativar(id) : service.inativar(id);
        return ResponseEntity.ok(mapper.toResponse(clube));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClubeResponse> update(@PathVariable @Positive Long id,
                                                @RequestBody @Valid ClubeUpdateRequest request) {
        Clube clube = mapper.toEntity(request);
        Clube updated = service.update(id, clube);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
