package br.gov.quixada.sports.registration;

import br.gov.quixada.sports.registration.dto.TeamRegistrationCreateRequest;
import br.gov.quixada.sports.registration.dto.TeamRegistrationNotesRequest;
import br.gov.quixada.sports.registration.dto.TeamRegistrationResponse;
import br.gov.quixada.sports.registration.dto.TeamRegistrationSummaryResponse;
import br.gov.quixada.sports.registration.dto.TeamRegistrationStatusRequest;
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
@RequestMapping(path = "/v{version}/team-registrations", version = "1.0")
@RequiredArgsConstructor
@Validated
public class TeamRegistrationController {

    private final TeamRegistrationService service;
    private final TeamRegistrationMapper mapper;

    @GetMapping
    public ResponseEntity<Page<TeamRegistrationSummaryResponse>> findAll(@RequestParam(required = false) Long competitionId,
                                                                       @RequestParam(required = false) RegistrationStatus status,
                                                                       Pageable pageable) {
        return ResponseEntity.ok(service.findAll(competitionId, status, pageable).map(mapper::toSummary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamRegistrationResponse> findById(@PathVariable @Positive Long id) {
        TeamRegistration registration = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(registration));
    }

    @PostMapping
    public ResponseEntity<TeamRegistrationResponse> save(@RequestBody @Valid TeamRegistrationCreateRequest request) {
        TeamRegistration registration = mapper.toEntity(request);
        TeamRegistration saved = service.save(registration, request.competitionId(), request.categoryId(), request.teamId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TeamRegistrationResponse> changeStatus(@PathVariable @Positive Long id,
                                                                 @RequestBody @Valid TeamRegistrationStatusRequest request) {
        TeamRegistration registration = service.changeStatus(id, request.status());
        return ResponseEntity.ok(mapper.toResponse(registration));
    }

    /**
     * Free-form note: independent of status and can be set, changed, or cleared at
     * any time. Blank or missing text clears the value.
     */
    @PatchMapping("/{id}/notes")
    public ResponseEntity<TeamRegistrationResponse> updateNotes(
            @PathVariable @Positive Long id,
            @RequestBody @Valid TeamRegistrationNotesRequest request) {
        TeamRegistration registration = service.updateNotes(id, request.notes());
        return ResponseEntity.ok(mapper.toResponse(registration));
    }
}
