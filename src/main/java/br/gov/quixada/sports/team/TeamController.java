package br.gov.quixada.sports.team;

import br.gov.quixada.sports.team.dto.TeamCreateRequest;
import br.gov.quixada.sports.team.dto.TeamResponse;
import br.gov.quixada.sports.team.dto.TeamSummaryResponse;
import br.gov.quixada.sports.team.dto.TeamStatusRequest;
import br.gov.quixada.sports.team.dto.TeamUpdateRequest;
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
@RequestMapping(path = "/v{version}/teams", version = "1.0")
@RequiredArgsConstructor
@Validated
public class TeamController {

    private final TeamService service;
    private final TeamMapper mapper;

    @GetMapping
    public ResponseEntity<Page<TeamSummaryResponse>> findAll(@RequestParam(required = false) String name,
                                                              @RequestParam(required = false) TeamStatus status,
                                                              Pageable pageable) {
        return ResponseEntity.ok(service.findAll(name, status, pageable).map(mapper::toSummary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> findById(@PathVariable @Positive Long id) {
        Team team = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(team));
    }

    @PostMapping
    public ResponseEntity<TeamResponse> save(@RequestBody @Valid TeamCreateRequest request) {
        Team team = mapper.toEntity(request);
        Team saved = service.save(team, request.clubId(), request.modalityId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TeamResponse> changeStatus(@PathVariable @Positive Long id,
                                                        @RequestBody @Valid TeamStatusRequest request) {
        Team team = request.status() == TeamStatus.ACTIVE ? service.activate(id) : service.deactivate(id);
        return ResponseEntity.ok(mapper.toResponse(team));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> update(@PathVariable @Positive Long id,
                                                 @RequestBody @Valid TeamUpdateRequest request) {
        Team team = mapper.toEntity(request);
        Team updated = service.update(id, team);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
