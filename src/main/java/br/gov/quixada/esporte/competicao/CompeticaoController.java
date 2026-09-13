package br.gov.quixada.esporte.competicao;

import br.gov.quixada.esporte.competicao.dto.CompeticaoCreateRequest;
import br.gov.quixada.esporte.competicao.dto.CompeticaoResponse;
import br.gov.quixada.esporte.competicao.dto.CompeticaoResumoResponse;
import br.gov.quixada.esporte.competicao.dto.CompeticaoStatusRequest;
import br.gov.quixada.esporte.competicao.dto.CompeticaoUpdateRequest;
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
@RequestMapping("/v1/competicoes")
@RequiredArgsConstructor
@Validated
public class CompeticaoController {

    private final CompeticaoService service;
    private final CompeticaoMapper mapper;

    @GetMapping
    public ResponseEntity<Page<CompeticaoResumoResponse>> findAll(@RequestParam(required = false) String nome, Pageable pageable) {
        return ResponseEntity.ok(service.findAll(nome, pageable).map(mapper::toResumo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompeticaoResponse> findById(@PathVariable @Positive Long id) {
        Competicao competicao = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(competicao));
    }

    @PostMapping
    public ResponseEntity<CompeticaoResponse> save(@RequestBody @Valid CompeticaoCreateRequest request) {
        Competicao competicao = mapper.toEntity(request);
        Competicao saved = service.save(competicao, request.modalidadeId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CompeticaoResponse> alterarStatus(@PathVariable @Positive Long id,
                                                            @RequestBody @Valid CompeticaoStatusRequest request) {
        Competicao competicao = service.alterarStatus(id, request.status());
        return ResponseEntity.ok(mapper.toResponse(competicao));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompeticaoResponse> update(@PathVariable @Positive Long id,
                                                     @RequestBody @Valid CompeticaoUpdateRequest request) {
        Competicao competicao = mapper.toEntity(request);
        Competicao updated = service.update(id, competicao);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
