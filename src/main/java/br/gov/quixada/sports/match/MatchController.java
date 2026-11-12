package br.gov.quixada.sports.match;

import br.gov.quixada.sports.match.dto.MatchCreateRequest;
import br.gov.quixada.sports.match.dto.MatchNotesRequest;
import br.gov.quixada.sports.match.dto.MatchResponse;
import br.gov.quixada.sports.match.dto.MatchResultRequest;
import br.gov.quixada.sports.match.dto.MatchSummaryResponse;
import br.gov.quixada.sports.match.dto.MatchStatusRequest;
import br.gov.quixada.sports.match.dto.MatchUpdateRequest;
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
@RequestMapping(path = "/v{version}/matches", version = "1.0")
@RequiredArgsConstructor
@Validated
public class MatchController {

    private final MatchService service;
    private final MatchMapper mapper;

    @GetMapping
    public ResponseEntity<Page<MatchSummaryResponse>> findAll(
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) MatchStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(service.findAll(competitionId, categoryId, status, pageable).map(mapper::toSummary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.findByIdOrThrowNotFound(id)));
    }

    @PostMapping
    public ResponseEntity<MatchResponse> save(@RequestBody @Valid MatchCreateRequest request) {
        Match match = service.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(match.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(match));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchResponse> update(@PathVariable @Positive Long id,
                                                  @RequestBody @Valid MatchUpdateRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MatchResponse> changeStatus(@PathVariable @Positive Long id,
                                                         @RequestBody @Valid MatchStatusRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.changeStatus(id, request.status())));
    }

    @PatchMapping("/{id}/result")
    public ResponseEntity<MatchResponse> recordResult(@PathVariable @Positive Long id,
                                                              @RequestBody @Valid MatchResultRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.recordResult(id, request)));
    }

    /**
     * Free-form note: independent of status and can be set, changed, or cleared at
     * any time. Blank or missing text clears the value.
     */
    @PatchMapping("/{id}/notes")
    public ResponseEntity<MatchResponse> updateNotes(
            @PathVariable @Positive Long id,
            @RequestBody @Valid MatchNotesRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.updateNotes(id, request.notes())));
    }
}
