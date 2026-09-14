package br.gov.quixada.esporte.inscricao;

import br.gov.quixada.esporte.inscricao.dto.InscricaoEquipeCreateRequest;
import br.gov.quixada.esporte.inscricao.dto.InscricaoEquipeResponse;
import br.gov.quixada.esporte.inscricao.dto.InscricaoEquipeResumoResponse;
import br.gov.quixada.esporte.inscricao.dto.InscricaoEquipeStatusRequest;
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
@RequestMapping("/v1/inscricoes-equipes")
@RequiredArgsConstructor
@Validated
public class InscricaoEquipeController {

    private final InscricaoEquipeService service;
    private final InscricaoEquipeMapper mapper;

    @GetMapping
    public ResponseEntity<Page<InscricaoEquipeResumoResponse>> findAll(@RequestParam(required = false) Long competicaoId, Pageable pageable) {
        return ResponseEntity.ok(service.findAll(competicaoId, pageable).map(mapper::toResumo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InscricaoEquipeResponse> findById(@PathVariable @Positive Long id) {
        InscricaoEquipe inscricao = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(inscricao));
    }

    @PostMapping
    public ResponseEntity<InscricaoEquipeResponse> save(@RequestBody @Valid InscricaoEquipeCreateRequest request) {
        InscricaoEquipe inscricao = mapper.toEntity(request);
        InscricaoEquipe saved = service.save(inscricao, request.competicaoId(), request.categoriaId(), request.equipeId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InscricaoEquipeResponse> alterarStatus(@PathVariable @Positive Long id,
                                                                 @RequestBody @Valid InscricaoEquipeStatusRequest request) {
        InscricaoEquipe inscricao = service.alterarStatus(id, request.status());
        return ResponseEntity.ok(mapper.toResponse(inscricao));
    }
}
