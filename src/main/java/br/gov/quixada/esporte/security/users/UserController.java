package br.gov.quixada.esporte.security.users;

import br.gov.quixada.esporte.security.users.dto.UserCreateRequest;
import br.gov.quixada.esporte.security.users.dto.UserResponse;
import br.gov.quixada.esporte.security.users.dto.UserStatusRequest;
import br.gov.quixada.esporte.security.users.dto.UserUpdateRequest;
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
@RequestMapping(path = "/v{version}/users", version = "1.0")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService service;
    private final UserMapper mapper;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> findAll(@RequestParam(required = false) String search,
                                                      @RequestParam(required = false) UserStatus status,
                                                      Pageable pageable) {
        return ResponseEntity.ok(service.findAll(search, status, pageable).map(mapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable @Positive Long id) {
        User user = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    @PostMapping
    public ResponseEntity<UserResponse> save(@RequestBody @Valid UserCreateRequest request) {
        User saved = service.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable @Positive Long id,
                                               @RequestBody @Valid UserUpdateRequest request) {
        User updated = service.update(id, request);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> alterarStatus(@PathVariable @Positive Long id,
                                                      @RequestBody @Valid UserStatusRequest request) {
        User user = service.alterarStatus(id, request.status());
        return ResponseEntity.ok(mapper.toResponse(user));
    }
}
