package br.gov.quixada.sports.common.error;

import java.time.LocalDateTime;
import java.util.List;

import br.gov.quixada.sports.athlete.exception.InactiveAthleteException;
import br.gov.quixada.sports.athlete.exception.AthleteNotDeletableException;
import br.gov.quixada.sports.athlete.exception.AthleteNotFoundException;
import br.gov.quixada.sports.athlete.exception.CpfAlreadyRegisteredException;
import br.gov.quixada.sports.category.exception.DuplicateCategoryException;
import br.gov.quixada.sports.category.exception.InactiveCategoryException;
import br.gov.quixada.sports.category.exception.CategoryNotFoundException;
import br.gov.quixada.sports.category.exception.InvalidAgeException;
import br.gov.quixada.sports.club.exception.InactiveClubException;
import br.gov.quixada.sports.club.exception.ClubNotFoundException;
import br.gov.quixada.sports.competition.exception.CompetitionNotEditableException;
import br.gov.quixada.sports.competition.exception.CompetitionNotFoundException;
import br.gov.quixada.sports.competition.exception.InvalidPeriodException;
import br.gov.quixada.sports.competition.exception.CompetitionStatusTransitionException;
import br.gov.quixada.sports.team.exception.InactiveTeamException;
import br.gov.quixada.sports.team.exception.TeamNotFoundException;
import br.gov.quixada.sports.registration.exception.AthleteOutsideCategoryException;
import br.gov.quixada.sports.registration.exception.AthleteAlreadyRegisteredException;
import br.gov.quixada.sports.registration.exception.TeamAlreadyRegisteredException;
import br.gov.quixada.sports.registration.exception.AthleteRegistrationNotFoundException;
import br.gov.quixada.sports.registration.exception.TeamRegistrationNotFoundException;
import br.gov.quixada.sports.registration.exception.TeamRegistrationWithoutAthletesException;
import br.gov.quixada.sports.registration.exception.InvalidRegistrationException;
import br.gov.quixada.sports.registration.exception.RegistrationClosedException;
import br.gov.quixada.sports.registration.exception.JerseyNumberAlreadyUsedException;
import br.gov.quixada.sports.registration.exception.InvalidStatusTransitionException;
import br.gov.quixada.sports.registration.exception.LastActiveAthleteException;
import br.gov.quixada.sports.modality.exception.InactiveModalityException;
import br.gov.quixada.sports.modality.exception.ModalityNotFoundException;
import br.gov.quixada.sports.match.exception.InvalidMatchException;
import br.gov.quixada.sports.match.exception.MatchNotFoundException;
import br.gov.quixada.sports.match.exception.MatchStatusTransitionException;
import br.gov.quixada.sports.security.auth.exception.EmailAlreadyRegisteredException;
import br.gov.quixada.sports.security.auth.exception.TooManyAttemptsException;
import br.gov.quixada.sports.security.users.exception.OperationNotAllowedException;
import br.gov.quixada.sports.security.users.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MSG_VALIDATION = "Validation error";
    private static final String MSG_INVALID_JSON = "Invalid JSON";
    private static final String MSG_DATA_CONFLICT = "Data conflict";

    @ExceptionHandler(AthleteNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AthleteNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CpfAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleCpfAlreadyRegistered(CpfAlreadyRegisteredException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<ApiError> handleOperationNotAllowed(OperationNotAllowedException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid credentials", request, List.of());
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ApiError> handleMuitasTentactives(TooManyAttemptsException ex, HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS,
                "Too many login attempts. Try again shortly.",
                request, List.of(), ex.retryAfterSeconds());
    }

    @ExceptionHandler(InactiveAthleteException.class)
    public ResponseEntity<ApiError> handleInactive(InactiveAthleteException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AthleteNotDeletableException.class)
    public ResponseEntity<ApiError> handleAthleteNaoExcluivel(AthleteNotDeletableException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiError> handleCategoryNotFound(CategoryNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CompetitionNotFoundException.class)
    public ResponseEntity<ApiError> handleCompetitionNotFound(CompetitionNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ClubNotFoundException.class)
    public ResponseEntity<ApiError> handleClubNotFound(ClubNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidAgeException.class)
    public ResponseEntity<ApiError> handleAgeInvalida(InvalidAgeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InactiveCategoryException.class)
    public ResponseEntity<ApiError> handleCategoryInactive(InactiveCategoryException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<ApiError> handleCategoryDuplicada(DuplicateCategoryException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InactiveClubException.class)
    public ResponseEntity<ApiError> handleClubInactive(InactiveClubException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<ApiError> handleTeamNotFound(TeamNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ModalityNotFoundException.class)
    public ResponseEntity<ApiError> handleModalityNotFound(ModalityNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InactiveTeamException.class)
    public ResponseEntity<ApiError> handleTeamInactive(InactiveTeamException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InactiveModalityException.class)
    public ResponseEntity<ApiError> handleModalityInactive(InactiveModalityException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(TeamRegistrationNotFoundException.class)
    public ResponseEntity<ApiError> handleTeamRegistrationNotFound(TeamRegistrationNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AthleteRegistrationNotFoundException.class)
    public ResponseEntity<ApiError> handleAthleteRegistrationNotFound(AthleteRegistrationNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidRegistrationException.class)
    public ResponseEntity<ApiError> handleInvalidRegistration(InvalidRegistrationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AthleteOutsideCategoryException.class)
    public ResponseEntity<ApiError> handleAthleteForaDaCategory(AthleteOutsideCategoryException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AthleteAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleAthleteJaInscrito(AthleteAlreadyRegisteredException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(TeamAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleTeamJaInscrita(TeamAlreadyRegisteredException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(TeamRegistrationWithoutAthletesException.class)
    public ResponseEntity<ApiError> handleTeamRegistrationSemAthletes(TeamRegistrationWithoutAthletesException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(LastActiveAthleteException.class)
    public ResponseEntity<ApiError> handleUltimoAthleteActive(LastActiveAthleteException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidStatusTransition(InvalidStatusTransitionException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ActiveReferenceException.class)
    public ResponseEntity<ApiError> handleActiveReference(ActiveReferenceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(JerseyNumberAlreadyUsedException.class)
    public ResponseEntity<ApiError> handleJerseyNumberJaUtilizado(JerseyNumberAlreadyUsedException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(RegistrationClosedException.class)
    public ResponseEntity<ApiError> handleRegistrationClosed(RegistrationClosedException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidPeriodException.class)
    public ResponseEntity<ApiError> handleInvalidPeriod(InvalidPeriodException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CompetitionNotEditableException.class)
    public ResponseEntity<ApiError> handleCompetitionNaoEditavel(CompetitionNotEditableException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CompetitionStatusTransitionException.class)
    public ResponseEntity<ApiError> handleCompetitionStatusTransition(CompetitionStatusTransitionException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<ApiError> handleMatchNotFound(MatchNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidMatchException.class)
    public ResponseEntity<ApiError> handleMatchInvalida(InvalidMatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MatchStatusTransitionException.class)
    public ResponseEntity<ApiError> handleMatchStatusTransition(MatchStatusTransitionException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldErrors> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldErrors(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDATION, request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldErrors> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldErrors(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDATION, request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, MSG_INVALID_JSON, request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter: " + ex.getName(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, MSG_DATA_CONFLICT, request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request, List<ApiError.FieldErrors> errors) {
        return build(status, message, request, errors, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request, List<ApiError.FieldErrors> errors,
                                           Long retryAfterSeconds) {
        ApiError body = new ApiError(status.value(), message, LocalDateTime.now(), request.getRequestURI(), errors);
        var response = ResponseEntity.status(status);
        if (retryAfterSeconds != null) {
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        }
        return response.body(body);
    }
}
