package br.gov.quixada.sports.registration;

import br.gov.quixada.sports.registration.dto.AthleteRegistrationCreateRequest;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationNotesRequest;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationResponse;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationSummaryResponse;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationStatusRequest;
import br.gov.quixada.sports.registration.dto.AthleteRegistrationUpdateRequest;
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
@RequestMapping(path = "/v{version}/athlete-registrations", version = "1.0")
@RequiredArgsConstructor
@Validated
public class AthleteRegistrationController {

    private final AthleteRegistrationService service;
    private final AthleteRegistrationMapper mapper;

    @GetMapping
    public ResponseEntity<Page<AthleteRegistrationSummaryResponse>> findAll(@RequestParam(required = false) Long teamRegistrationId,
                                                                       @RequestParam(required = false) RegistrationStatus status,
                                                                       Pageable pageable) {
        return ResponseEntity.ok(service.findAll(teamRegistrationId, status, pageable).map(mapper::toSummary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AthleteRegistrationResponse> findById(@PathVariable @Positive Long id) {
        AthleteRegistration registration = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(registration));
    }

    @PostMapping
    public ResponseEntity<AthleteRegistrationResponse> save(@RequestBody @Valid AthleteRegistrationCreateRequest request) {
        AthleteRegistration registration = mapper.toEntity(request);
        AthleteRegistration saved = service.save(registration, request.teamRegistrationId(), request.athleteId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AthleteRegistrationResponse> changeStatus(@PathVariable @Positive Long id,
                                                                 @RequestBody @Valid AthleteRegistrationStatusRequest request) {
        AthleteRegistration registration = service.changeStatus(id, request.status());
        return ResponseEntity.ok(mapper.toResponse(registration));
    }

    /**
     * Free-form note: independent of status and can be set, changed, or cleared at
     * any time. Blank or missing text clears the value.
     */
    @PatchMapping("/{id}/notes")
    public ResponseEntity<AthleteRegistrationResponse> updateNotes(
            @PathVariable @Positive Long id,
            @RequestBody @Valid AthleteRegistrationNotesRequest request) {
        AthleteRegistration registration = service.updateNotes(id, request.notes());
        return ResponseEntity.ok(mapper.toResponse(registration));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AthleteRegistrationResponse> update(@PathVariable @Positive Long id,
                                                         @RequestBody @Valid AthleteRegistrationUpdateRequest request) {
        AthleteRegistration registration = service.update(id, request.jerseyNumber());
        return ResponseEntity.ok(mapper.toResponse(registration));
    }
}
