package br.gov.quixada.sports.club;

import br.gov.quixada.sports.club.dto.ClubCreateRequest;
import br.gov.quixada.sports.club.dto.ClubResponse;
import br.gov.quixada.sports.club.dto.ClubSummaryResponse;
import br.gov.quixada.sports.club.dto.ClubStatusRequest;
import br.gov.quixada.sports.club.dto.ClubUpdateRequest;
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
@RequestMapping(path = "/v{version}/clubs", version = "1.0")
@RequiredArgsConstructor
@Validated
public class ClubController {

    private final ClubService service;
    private final ClubMapper mapper;

    @GetMapping
    public ResponseEntity<Page<ClubSummaryResponse>> findAll(@RequestParam(required = false) String name,
                                                             @RequestParam(required = false) ClubStatus status,
                                                             Pageable pageable) {
        return ResponseEntity.ok(service.findAll(name, status, pageable).map(mapper::toSummary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClubResponse> findById(@PathVariable @Positive Long id) {
        Club club = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(club));
    }

    @PostMapping
    public ResponseEntity<ClubResponse> save(@RequestBody @Valid ClubCreateRequest request) {
        Club club = mapper.toEntity(request);
        Club saved = service.save(club);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ClubResponse> changeStatus(@PathVariable @Positive Long id,
                                                       @RequestBody @Valid ClubStatusRequest request) {
        Club club = request.status() == ClubStatus.ACTIVE ? service.activate(id) : service.deactivate(id);
        return ResponseEntity.ok(mapper.toResponse(club));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClubResponse> update(@PathVariable @Positive Long id,
                                                @RequestBody @Valid ClubUpdateRequest request) {
        Club club = mapper.toEntity(request);
        Club updated = service.update(id, club);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
