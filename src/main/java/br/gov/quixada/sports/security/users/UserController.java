package br.gov.quixada.sports.security.users;

import br.gov.quixada.sports.security.users.dto.UserAuditResponse;
import br.gov.quixada.sports.security.users.dto.UserCreateRequest;
import br.gov.quixada.sports.security.users.dto.UserResponse;
import br.gov.quixada.sports.security.users.dto.UserStatusRequest;
import br.gov.quixada.sports.security.users.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(path = "/v{version}/users", version = "1.0")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "User administration and audit logs")
public class UserController {

    private final UserService service;
    private final UserAuditService auditService;
    private final UserMapper mapper;

    @Operation(summary = "List users with optional search and status filter")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> findAll(@RequestParam(required = false) String search,
                                                      @RequestParam(required = false) UserStatus status,
                                                      Pageable pageable) {
        return ResponseEntity.ok(service.findAll(search, status, pageable).map(mapper::toResponse));
    }

    @Operation(summary = "Search user audit logs with combinable filters")
    @GetMapping("/audit")
    public ResponseEntity<Page<UserAuditResponse>> findAudit(
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String targetEmail,
            @RequestParam(required = false) UserAuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.findAll(actorEmail, targetEmail, action, from, to, pageable)
                .map(UserAuditResponse::from));
    }

    @Operation(summary = "Export user audit logs in CSV or JSON format")
    @GetMapping("/audit/export")
    public ResponseEntity<?> exportAudit(
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String targetEmail,
            @RequestParam(required = false) UserAuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "CSV") UserAuditExportFormat format) {
        List<UserAudit> rows = auditService.findForExport(actorEmail, targetEmail, action, from, to);
        if (format == UserAuditExportFormat.JSON) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user-audit.json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rows.stream().map(UserAuditResponse::from).toList());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user-audit.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(auditService.exportCsv(rows));
    }

    @Operation(summary = "Find user by ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable @Positive Long id) {
        User user = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    @Operation(summary = "Create a new user")
    @PostMapping
    public ResponseEntity<UserResponse> save(@AuthenticationPrincipal UserDetailsAdapter actor,
                                             @RequestBody @Valid UserCreateRequest request) {
        User saved = service.create(actor.user(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @Operation(summary = "Update user by ID")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@AuthenticationPrincipal UserDetailsAdapter actor,
                                               @PathVariable @Positive Long id,
                                               @RequestBody @Valid UserUpdateRequest request) {
        User updated = service.update(actor.user(), id, request);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @Operation(summary = "Change user status (soft delete / reactivate)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> changeStatus(@AuthenticationPrincipal UserDetailsAdapter actor,
                                                      @PathVariable @Positive Long id,
                                                      @RequestBody @Valid UserStatusRequest request) {
        User user = service.changeStatus(actor.user(), id, request.status());
        return ResponseEntity.ok(mapper.toResponse(user));
    }
}
