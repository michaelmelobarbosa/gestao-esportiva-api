package br.gov.quixada.esporte.modalidade;

import br.gov.quixada.esporte.modalidade.dto.ModalidadeCreateRequest;
import br.gov.quixada.esporte.modalidade.dto.ModalidadeResponse;
import br.gov.quixada.esporte.modalidade.dto.ModalidadeResumoResponse;
import br.gov.quixada.esporte.modalidade.dto.ModalidadeStatusRequest;
import br.gov.quixada.esporte.modalidade.dto.ModalidadeUpdateRequest;
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
@RequestMapping("/v1/modalidades")
@RequiredArgsConstructor
@Validated
public class ModalidadeController {

    private final ModalidadeService service;
    private final ModalidadeMapper mapper;

    @GetMapping
    public ResponseEntity<Page<ModalidadeResumoResponse>> findAll(@RequestParam(required = false) String nome, Pageable pageable) {
        return ResponseEntity.ok(service.findAll(nome, pageable).map(mapper::toResumo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModalidadeResponse> findById(@PathVariable @Positive Long id) {
        Modalidade modalidade = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(modalidade));
    }

    @PostMapping
    public ResponseEntity<ModalidadeResponse> save(@RequestBody @Valid ModalidadeCreateRequest request) {
        Modalidade modalidade = mapper.toEntity(request);
        Modalidade saved = service.save(modalidade);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ModalidadeResponse> alterarStatus(@PathVariable @Positive Long id,
                                                            @RequestBody @Valid ModalidadeStatusRequest request) {
        Modalidade modalidade = request.status() == StatusModalidade.ATIVO ? service.ativar(id) : service.inativar(id);
        return ResponseEntity.ok(mapper.toResponse(modalidade));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModalidadeResponse> update(@PathVariable @Positive Long id,
                                                     @RequestBody @Valid ModalidadeUpdateRequest request) {
        Modalidade modalidade = mapper.toEntity(request);
        Modalidade updated = service.update(id, modalidade);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
