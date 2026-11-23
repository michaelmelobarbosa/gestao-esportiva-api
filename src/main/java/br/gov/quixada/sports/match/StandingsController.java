package br.gov.quixada.sports.match;

import br.gov.quixada.sports.match.dto.StandingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Standings", description = "Competition category standings")
public class StandingsController {

    private final StandingsService service;

    @Operation(summary = "Get standings for a competition category")
    @GetMapping
    public ResponseEntity<List<StandingsResponse>> getStandings(@PathVariable @Positive Long competitionId,
                                                                 @PathVariable @Positive Long categoryId) {
        return ResponseEntity.ok(service.calculate(competitionId, categoryId));
    }
}
