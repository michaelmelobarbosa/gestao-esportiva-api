package br.gov.quixada.sports.category;

import br.gov.quixada.sports.category.CategoryStatus;
import br.gov.quixada.sports.category.dto.CategoryCreateRequest;
import br.gov.quixada.sports.category.dto.CategoryResponse;
import br.gov.quixada.sports.category.dto.CategorySummaryResponse;
import br.gov.quixada.sports.category.dto.CategoryStatusRequest;
import br.gov.quixada.sports.category.dto.CategoryUpdateRequest;
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
@RequestMapping(path = "/v{version}/categories", version = "1.0")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService service;
    private final CategoryMapper mapper;

    @GetMapping
    public ResponseEntity<Page<CategorySummaryResponse>> findAll(@RequestParam(required = false) String name,
                                                                 @RequestParam(required = false) CategoryStatus status,
                                                                 Pageable pageable) {
        return ResponseEntity.ok(service.findAll(name, status, pageable).map(mapper::toSummary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable @Positive Long id) {
        Category category = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(category));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> save(@RequestBody @Valid CategoryCreateRequest request) {
        Category category = mapper.toEntity(request);
        Category saved = service.save(category, request.competitionId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CategoryResponse> changeStatus(@PathVariable @Positive Long id,
                                                           @RequestBody @Valid CategoryStatusRequest request) {
        Category category = request.status() == CategoryStatus.ACTIVE ? service.activate(id) : service.deactivate(id);
        return ResponseEntity.ok(mapper.toResponse(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable @Positive Long id,
                                                    @RequestBody @Valid CategoryUpdateRequest request) {
        Category category = mapper.toEntity(request);
        Category updated = service.update(id, category);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
