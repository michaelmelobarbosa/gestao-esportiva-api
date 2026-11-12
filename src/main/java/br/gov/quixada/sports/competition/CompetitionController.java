package br.gov.quixada.sports.competition;

import br.gov.quixada.sports.competition.dto.CompetitionCreateRequest;
import br.gov.quixada.sports.competition.dto.CompetitionNotesRequest;
import br.gov.quixada.sports.competition.dto.CompetitionResponse;
import br.gov.quixada.sports.competition.dto.CompetitionSummaryResponse;
import br.gov.quixada.sports.competition.dto.CompetitionStatusRequest;
import br.gov.quixada.sports.competition.dto.CompetitionUpdateRequest;
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
@RequestMapping(path = "/v{version}/competitions", version = "1.0")
@RequiredArgsConstructor
@Validated
public class CompetitionController {

    private final CompetitionService service;
    private final CompetitionMapper mapper;

    @GetMapping
    public ResponseEntity<Page<CompetitionSummaryResponse>> findAll(@RequestParam(required = false) String name,
                                                                  @RequestParam(required = false) CompetitionStatus status,
                                                                  Pageable pageable) {
        return ResponseEntity.ok(service.findAll(name, status, pageable).map(mapper::toSummary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionResponse> findById(@PathVariable @Positive Long id) {
        Competition competition = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(competition));
    }

    @PostMapping
    public ResponseEntity<CompetitionResponse> save(@RequestBody @Valid CompetitionCreateRequest request) {
        Competition competition = mapper.toEntity(request);
        Competition saved = service.save(competition, request.modalityId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CompetitionResponse> changeStatus(@PathVariable @Positive Long id,
                                                            @RequestBody @Valid CompetitionStatusRequest request) {
        Competition competition = service.changeStatus(id, request.status());
        return ResponseEntity.ok(mapper.toResponse(competition));
    }

    /**
     * Free-form note: independent of status and can be set, changed, or cleared at
     * any time. Blank or missing text clears the value.
     */
    @PatchMapping("/{id}/notes")
    public ResponseEntity<CompetitionResponse> updateNotes(
            @PathVariable @Positive Long id,
            @RequestBody @Valid CompetitionNotesRequest request) {
        Competition competition = service.updateNotes(id, request.notes());
        return ResponseEntity.ok(mapper.toResponse(competition));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionResponse> update(@PathVariable @Positive Long id,
                                                     @RequestBody @Valid CompetitionUpdateRequest request) {
        Competition competition = mapper.toEntity(request);
        Competition updated = service.update(id, competition);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
