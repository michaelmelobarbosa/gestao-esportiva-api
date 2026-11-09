package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.inscricao.dto.InscricaoAtletaCreateRequest;
import br.gov.quixada.esporte.inscricao.dto.InscricaoAtletaResponse;
import br.gov.quixada.esporte.inscricao.dto.InscricaoAtletaResumoResponse;
import br.gov.quixada.esporte.inscricao.dto.InscricaoAtletaStatusRequest;
import br.gov.quixada.esporte.inscricao.dto.InscricaoAtletaUpdateRequest;
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
@RequestMapping(path = "/v{version}/inscricoes-atletas", version = "1.0")
@RequiredArgsConstructor
@Validated
public class InscricaoAtletaController {

    private final InscricaoAtletaService service;
    private final InscricaoAtletaMapper mapper;

    @GetMapping
    public ResponseEntity<Page<InscricaoAtletaResumoResponse>> findAll(@RequestParam(required = false) Long inscricaoEquipeId,
                                                                       @RequestParam(required = false) StatusInscricao status,
                                                                       Pageable pageable) {
        return ResponseEntity.ok(service.findAll(inscricaoEquipeId, status, pageable).map(mapper::toResumo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InscricaoAtletaResponse> findById(@PathVariable @Positive Long id) {
        InscricaoAtleta inscricao = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(inscricao));
    }

    @PostMapping
    public ResponseEntity<InscricaoAtletaResponse> save(@RequestBody @Valid InscricaoAtletaCreateRequest request) {
        InscricaoAtleta inscricao = mapper.toEntity(request);
        InscricaoAtleta saved = service.save(inscricao, request.inscricaoEquipeId(), request.atletaId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InscricaoAtletaResponse> alterarStatus(@PathVariable @Positive Long id,
                                                                 @RequestBody @Valid InscricaoAtletaStatusRequest request) {
        InscricaoAtleta inscricao = service.alterarStatus(id, request.status());
        return ResponseEntity.ok(mapper.toResponse(inscricao));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InscricaoAtletaResponse> update(@PathVariable @Positive Long id,
                                                         @RequestBody @Valid InscricaoAtletaUpdateRequest request) {
        InscricaoAtleta inscricao = service.update(id, request.numeroCamisa());
        return ResponseEntity.ok(mapper.toResponse(inscricao));
    }
}
