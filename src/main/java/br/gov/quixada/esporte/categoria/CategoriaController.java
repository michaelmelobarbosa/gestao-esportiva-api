package br.gov.quixada.esporte.categoria;

import br.gov.quixada.esporte.categoria.dto.CategoriaCreateRequest;
import br.gov.quixada.esporte.categoria.dto.CategoriaResponse;
import br.gov.quixada.esporte.categoria.dto.CategoriaResumoResponse;
import br.gov.quixada.esporte.categoria.dto.CategoriaUpdateRequest;
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
@RequestMapping("/v1/categorias")
@RequiredArgsConstructor
@Validated
public class CategoriaController {

    private final CategoriaService service;
    private final CategoriaMapper mapper;

    @GetMapping
    public ResponseEntity<Page<CategoriaResumoResponse>> findAll(@RequestParam(required = false) String nome, Pageable pageable) {
        return ResponseEntity.ok(service.findAll(nome, pageable).map(mapper::toResumo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> findById(@PathVariable @Positive Long id) {
        Categoria categoria = service.findByIdOrThrowNotFound(id);
        return ResponseEntity.ok(mapper.toResponse(categoria));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> save(@RequestBody @Valid CategoriaCreateRequest request) {
        Categoria categoria = mapper.toEntity(request);
        Categoria saved = service.save(categoria, request.competicaoId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> update(@PathVariable @Positive Long id,
                                                    @RequestBody @Valid CategoriaUpdateRequest request) {
        Categoria categoria = mapper.toEntity(request);
        Categoria updated = service.update(id, categoria);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
