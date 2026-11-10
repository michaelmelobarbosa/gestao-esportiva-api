package br.gov.quixada.esporte.partida;

import br.gov.quixada.esporte.partida.dto.PartidaCreateRequest;
import br.gov.quixada.esporte.partida.dto.PartidaObservacaoRequest;
import br.gov.quixada.esporte.partida.dto.PartidaResponse;
import br.gov.quixada.esporte.partida.dto.PartidaResultadoRequest;
import br.gov.quixada.esporte.partida.dto.PartidaResumoResponse;
import br.gov.quixada.esporte.partida.dto.PartidaStatusRequest;
import br.gov.quixada.esporte.partida.dto.PartidaUpdateRequest;
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
@RequestMapping(path = "/v{version}/partidas", version = "1.0")
@RequiredArgsConstructor
@Validated
public class PartidaController {

    private final PartidaService service;
    private final PartidaMapper mapper;

    @GetMapping
    public ResponseEntity<Page<PartidaResumoResponse>> findAll(
            @RequestParam(required = false) Long competicaoId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) StatusPartida status,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(competicaoId, categoriaId, status, pageable).map(mapper::toResumo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartidaResponse> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.findByIdOrThrowNotFound(id)));
    }

    @PostMapping
    public ResponseEntity<PartidaResponse> save(@RequestBody @Valid PartidaCreateRequest request) {
        Partida partida = service.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(partida.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(partida));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartidaResponse> update(@PathVariable @Positive Long id,
                                                  @RequestBody @Valid PartidaUpdateRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PartidaResponse> alterarStatus(@PathVariable @Positive Long id,
                                                         @RequestBody @Valid PartidaStatusRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.alterarStatus(id, request.status())));
    }

    @PatchMapping("/{id}/resultado")
    public ResponseEntity<PartidaResponse> registrarResultado(@PathVariable @Positive Long id,
                                                              @RequestBody @Valid PartidaResultadoRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.registrarResultado(id, request)));
    }

    /**
     * Anotação livre: independe do status e pode ser criada, alterada ou limpa a
     * qualquer momento. Texto em branco/ausente limpa o valor.
     */
    @PatchMapping("/{id}/observacao")
    public ResponseEntity<PartidaResponse> atualizarObservacao(
            @PathVariable @Positive Long id,
            @RequestBody @Valid PartidaObservacaoRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.atualizarObservacao(id, request.observacao())));
    }
}
