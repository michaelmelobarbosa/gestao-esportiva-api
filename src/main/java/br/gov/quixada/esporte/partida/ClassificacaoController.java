package br.gov.quixada.esporte.partida;

import br.gov.quixada.esporte.partida.dto.ClassificacaoResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
        path = "/v{version}/competicoes/{competicaoId}/categorias/{categoriaId}/classificacao",
        version = "1.0")
@RequiredArgsConstructor
@Validated
public class ClassificacaoController {

    private final ClassificacaoService service;

    @GetMapping
    public ResponseEntity<List<ClassificacaoResponse>> obter(@PathVariable @Positive Long competicaoId,
                                                             @PathVariable @Positive Long categoriaId) {
        return ResponseEntity.ok(service.calcular(competicaoId, categoriaId));
    }
}
