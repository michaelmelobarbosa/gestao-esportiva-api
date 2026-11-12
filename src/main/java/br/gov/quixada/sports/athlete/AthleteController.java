package br.gov.quixada.sports.athlete;


import br.gov.quixada.sports.athlete.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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
@RequestMapping(path = "/v{version}/athletes", version = "1.0")
@RequiredArgsConstructor
@Validated
public class AthleteController {

    private final AthleteService service;
    private final AthleteMapper mapper;
    private final Clock clock;

    @GetMapping
    public ResponseEntity<Page<AthleteSummaryResponse>> findAll(@RequestParam(required = false) String name,
                                                              @RequestParam(required = false) AthleteStatus status,
                                                              Pageable pageable) {

        return ResponseEntity.ok(service.findAll(name, status, pageable).map(athlete -> mapper.toSummary(athlete, clock)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AthleteResponse> findById(@PathVariable @Positive Long id) {
        Athlete athlete = service.findByIdOrThrowNotFound(id);
        AthleteResponse response = mapper.toGetResponse(athlete, clock);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<AthleteResponse> findByCpf(@PathVariable
                                                    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11}",
                                                            message = "Invalid CPF")
                                                    String cpf) {
        Athlete athlete = service.findByCpfOrThrowNotFound(cpf);
        return ResponseEntity.ok(mapper.toGetResponse(athlete, clock));
    }

    @PostMapping
    public ResponseEntity<AthleteResponse> save(@RequestBody @Valid AthleteCreateRequest request) {
        Athlete athlete = mapper.toEntity(request);
        Athlete saved = service.save(athlete);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toGetResponse(saved, clock));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AthleteResponse> changeStatus(@PathVariable @Positive Long id,
                                                        @RequestBody @Valid AthleteStatusRequest request) {

        Athlete athlete = request.status() == AthleteStatus.ACTIVE ? service.activate(id) : service.deactivate(id);

        return ResponseEntity.ok(mapper.toGetResponse(athlete, clock));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AthleteResponse> update(@PathVariable @Positive Long id,
                                                 @RequestBody @Valid AthleteUpdateRequest request) {

        Athlete athlete = mapper.toEntity(request);
        Athlete updated = service.update(id, athlete);
        AthleteResponse getResponse = mapper.toGetResponse(updated, clock);

        return ResponseEntity.ok(getResponse);
    }

    /**
     * Physical deletion: requires an INACTIVE athlete with no registrations occupying a slot.
     * CANCELED/REJECTED registrations are removed as well.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hardDelete(@PathVariable @Positive Long id) {
        service.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}
