package br.gov.quixada.esporte.atleta;


import br.gov.quixada.esporte.atleta.dto.*;
import br.gov.quixada.esporte.extras.StatusAtleta;
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
import java.time.Clock;

@RestController
@RequestMapping("/v1/atletas")
@RequiredArgsConstructor
@Validated
public class AtletaController {

    private final AtletaService service;
    private final AtletaMapper mapper;
    private final Clock clock;


    @GetMapping
    public ResponseEntity<Page<AtletaResumoResponse>> findAll(@RequestParam(required = false) String nome, Pageable pageable) {

        return ResponseEntity.ok(service.findAll(nome, pageable).map(atleta -> mapper.toResumo(atleta, clock)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtletaResponse> findById(@PathVariable @Positive Long id) {
        Atleta atleta = service.findByIdOrThrowNotFound(id);
        AtletaResponse response = mapper.toGetResponse(atleta, clock);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AtletaResponse> save(@RequestBody @Valid AtletaCreateRequest request) {
        Atleta atleta = mapper.toEntity(request);
        Atleta saved = service.save(atleta);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toGetResponse(saved, clock));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AtletaResponse> alterarStatus(@PathVariable @Positive Long id,
                                                        @RequestBody @Valid AtletaStatusRequest request) {

        Atleta atleta = request.status() == StatusAtleta.ATIVO ? service.ativar(id) : service.inativar(id);

        return ResponseEntity.ok(mapper.toGetResponse(atleta, clock));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtletaResponse> update(@PathVariable @Positive Long id,
                                                 @RequestBody @Valid AtletaUpdateRequest request) {

        Atleta atleta = mapper.toEntity(request);
        Atleta updated = service.update(id, atleta);
        AtletaResponse getResponse = mapper.toGetResponse(updated, clock);

        return ResponseEntity.ok(getResponse);
    }
}
