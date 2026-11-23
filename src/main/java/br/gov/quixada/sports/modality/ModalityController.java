package br.gov.quixada.sports.modality;

import br.gov.quixada.sports.modality.dto.ModalityCreateRequest;
import br.gov.quixada.sports.modality.dto.ModalityResponse;
import br.gov.quixada.sports.modality.dto.ModalitySummaryResponse;
import br.gov.quixada.sports.modality.dto.ModalityStatusRequest;
import br.gov.quixada.sports.modality.dto.ModalityUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping(path = "/v{version}/modalities", version = "1.0")
@RequiredArgsConstructor
@Validated
@Tag(name = "Modalities", description = "Sports modalities management")
public class ModalityController {

    private final ModalityService service;
    private final ModalityMapper mapper;

    @Operation(summary = "List modalities with optional name and status filter")
    @GetMapping
    public ResponseEntity<Page<ModalitySummaryResponse>> findAll(@RequestParam(required = false) String name,
                                                                  @RequestParam(required = false) ModalityStatus status,
                                                                  Pageable pageable) {
        return ResponseEntity.ok(service.findAll(name, status, pageable).map(mapper::toSummary));
    }

    @Operation(summary = "Find modality by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ModalityResponse> findById(@PathVariable @Positive Long id) {
        Modality modality = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(modality));
    }

    @Operation(summary = "Create a new modality")
    @PostMapping
    public ResponseEntity<ModalityResponse> save(@RequestBody @Valid ModalityCreateRequest request) {
        Modality modality = mapper.toEntity(request);
        Modality saved = service.save(modality);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @Operation(summary = "Change modality status (activate / deactivate)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ModalityResponse> changeStatus(@PathVariable @Positive Long id,
                                                            @RequestBody @Valid ModalityStatusRequest request) {
        Modality modality = request.status() == ModalityStatus.ACTIVE ? service.activate(id) : service.deactivate(id);
        return ResponseEntity.ok(mapper.toResponse(modality));
    }

    @Operation(summary = "Update modality by ID")
    @PutMapping("/{id}")
    public ResponseEntity<ModalityResponse> update(@PathVariable @Positive Long id,
                                                     @RequestBody @Valid ModalityUpdateRequest request) {
        Modality modality = mapper.toEntity(request);
        Modality updated = service.update(id, modality);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
