package br.gov.quixada.sports.match;

import br.gov.quixada.sports.match.dto.StandingsResponse;
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
        path = "/v{version}/competitions/{competitionId}/categories/{categoryId}/standings",
        version = "1.0")
@RequiredArgsConstructor
@Validated
public class StandingsController {

    private final StandingsService service;

    @GetMapping
    public ResponseEntity<List<StandingsResponse>> obter(@PathVariable @Positive Long competitionId,
                                                             @PathVariable @Positive Long categoryId) {
        return ResponseEntity.ok(service.calculate(competitionId, categoryId));
    }
}
