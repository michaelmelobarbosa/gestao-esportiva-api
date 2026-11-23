package br.gov.quixada.sports.match;

import br.gov.quixada.sports.match.dto.MatchScheduleGenerateRequest;
import br.gov.quixada.sports.match.dto.MatchScheduleGenerateResponse;
import br.gov.quixada.sports.match.dto.MatchSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
        path = "/v{version}/competitions/{competitionId}/categories/{categoryId}/matches",
        version = "1.0")
@RequiredArgsConstructor
@Validated
@Tag(name = "Match Schedule", description = "Automatic round-robin schedule generation")
public class MatchScheduleController {

    private final MatchService service;
    private final MatchMapper mapper;

    @Operation(summary = "Generate round-robin match schedule for approved team registrations")
    @PostMapping("/generate")
    public ResponseEntity<MatchScheduleGenerateResponse> generate(
            @PathVariable @Positive Long competitionId,
            @PathVariable @Positive Long categoryId,
            @RequestBody(required = false) @Valid MatchScheduleGenerateRequest request) {
        MatchScheduleGenerateRequest effectiveRequest = request != null
                ? request
                : new MatchScheduleGenerateRequest(false, null, null, null);

        List<Match> generated = service.generateSchedule(competitionId, categoryId, effectiveRequest);
        int totalRounds = generated.stream().mapToInt(Match::getRound).max().orElse(0);
        List<MatchSummaryResponse> summaries = generated.stream().map(mapper::toSummary).toList();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MatchScheduleGenerateResponse(totalRounds, summaries.size(), summaries));
    }
}
