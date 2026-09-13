package br.gov.quixada.esporte.equipe;

import br.gov.quixada.esporte.equipe.dto.EquipeCreateRequest;
import br.gov.quixada.esporte.equipe.dto.EquipeResponse;
import br.gov.quixada.esporte.equipe.dto.EquipeResumoResponse;
import br.gov.quixada.esporte.equipe.dto.EquipeStatusRequest;
import br.gov.quixada.esporte.equipe.dto.EquipeUpdateRequest;
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
@RequestMapping("/v1/equipes")
@RequiredArgsConstructor
@Validated
public class EquipeController {

    private final EquipeService service;
    private final EquipeMapper mapper;

    @GetMapping
    public ResponseEntity<Page<EquipeResumoResponse>> findAll(@RequestParam(required = false) String nome, Pageable pageable) {
        return ResponseEntity.ok(service.findAll(nome, pageable).map(mapper::toResumo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipeResponse> findById(@PathVariable @Positive Long id) {
        Equipe equipe = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(equipe));
    }

    @PostMapping
    public ResponseEntity<EquipeResponse> save(@RequestBody @Valid EquipeCreateRequest request) {
        Equipe equipe = mapper.toEntity(request);
        Equipe saved = service.save(equipe, request.clubeId(), request.modalidadeId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EquipeResponse> alterarStatus(@PathVariable @Positive Long id,
                                                        @RequestBody @Valid EquipeStatusRequest request) {
        Equipe equipe = request.status() == StatusEquipe.ATIVO ? service.ativar(id) : service.inativar(id);
        return ResponseEntity.ok(mapper.toResponse(equipe));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipeResponse> update(@PathVariable @Positive Long id,
                                                 @RequestBody @Valid EquipeUpdateRequest request) {
        Equipe equipe = mapper.toEntity(request);
        Equipe updated = service.update(id, equipe);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
