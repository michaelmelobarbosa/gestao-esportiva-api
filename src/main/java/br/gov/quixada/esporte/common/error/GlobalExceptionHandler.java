package br.gov.quixada.esporte.common.error;

import java.time.LocalDateTime;
import java.util.List;

import br.gov.quixada.esporte.atleta.exception.AtletaInativoException;
import br.gov.quixada.esporte.atleta.exception.AtletaNaoExcluivelException;
import br.gov.quixada.esporte.atleta.exception.AtletaNotFoundException;
import br.gov.quixada.esporte.atleta.exception.CpfJaCadastradoException;
import br.gov.quixada.esporte.categoria.exception.CategoriaDuplicadaException;
import br.gov.quixada.esporte.categoria.exception.CategoriaInativaException;
import br.gov.quixada.esporte.categoria.exception.CategoriaNotFoundException;
import br.gov.quixada.esporte.categoria.exception.IdadeInvalidaException;
import br.gov.quixada.esporte.clube.exception.ClubeInativoException;
import br.gov.quixada.esporte.clube.exception.ClubeNotFoundException;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNaoEditavelException;
import br.gov.quixada.esporte.competicao.exception.CompeticaoNotFoundException;
import br.gov.quixada.esporte.competicao.exception.PeriodoInvalidoException;
import br.gov.quixada.esporte.competicao.exception.TransicaoStatusCompeticaoException;
import br.gov.quixada.esporte.equipe.exception.EquipeInativaException;
import br.gov.quixada.esporte.equipe.exception.EquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.AtletaForaDaCategoriaException;
import br.gov.quixada.esporte.inscricao.exception.AtletaJaInscritoException;
import br.gov.quixada.esporte.inscricao.exception.EquipeJaInscritaException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoAtletaNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeNotFoundException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoEquipeSemAtletasException;
import br.gov.quixada.esporte.inscricao.exception.InscricaoInvalidaException;
import br.gov.quixada.esporte.inscricao.exception.InscricoesEncerradasException;
import br.gov.quixada.esporte.inscricao.exception.NumeroCamisaJaUtilizadoException;
import br.gov.quixada.esporte.inscricao.exception.TransicaoStatusInvalidaException;
import br.gov.quixada.esporte.inscricao.exception.UltimoAtletaAtivoException;
import br.gov.quixada.esporte.modalidade.exception.ModalidadeInativaException;
import br.gov.quixada.esporte.modalidade.exception.ModalidadeNotFoundException;
import br.gov.quixada.esporte.partida.exception.PartidaInvalidaException;
import br.gov.quixada.esporte.partida.exception.PartidaNotFoundException;
import br.gov.quixada.esporte.partida.exception.TransicaoStatusPartidaException;
import br.gov.quixada.esporte.security.auth.exception.EmailJaCadastradoException;
import br.gov.quixada.esporte.security.users.exception.OperacaoNaoPermitidaException;
import br.gov.quixada.esporte.security.users.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final String MSG_VALIDACAO = "Erro de validação";
    private static final String MSG_JSON_INVALIDO = "JSON inválido";
    private static final String MSG_CONFLITO_DADOS = "Conflito de dados";

    @ExceptionHandler(AtletaNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AtletaNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CpfJaCadastradoException.class)
    public ResponseEntity<ApiError> handleCpfCadastrado(CpfJaCadastradoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ApiError> handleEmailJaCadastrado(EmailJaCadastradoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ResponseEntity<ApiError> handleOperacaoNaoPermitida(OperacaoNaoPermitidaException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", request, List.of());
    }

    @ExceptionHandler(AtletaInativoException.class)
    public ResponseEntity<ApiError> handleInativo(AtletaInativoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AtletaNaoExcluivelException.class)
    public ResponseEntity<ApiError> handleAtletaNaoExcluivel(AtletaNaoExcluivelException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CategoriaNotFoundException.class)
    public ResponseEntity<ApiError> handleCategoriaNotFound(CategoriaNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CompeticaoNotFoundException.class)
    public ResponseEntity<ApiError> handleCompeticaoNotFound(CompeticaoNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ClubeNotFoundException.class)
    public ResponseEntity<ApiError> handleClubeNotFound(ClubeNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(IdadeInvalidaException.class)
    public ResponseEntity<ApiError> handleIdadeInvalida(IdadeInvalidaException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CategoriaInativaException.class)
    public ResponseEntity<ApiError> handleCategoriaInativa(CategoriaInativaException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CategoriaDuplicadaException.class)
    public ResponseEntity<ApiError> handleCategoriaDuplicada(CategoriaDuplicadaException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ClubeInativoException.class)
    public ResponseEntity<ApiError> handleClubeInativo(ClubeInativoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(EquipeNotFoundException.class)
    public ResponseEntity<ApiError> handleEquipeNotFound(EquipeNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ModalidadeNotFoundException.class)
    public ResponseEntity<ApiError> handleModalidadeNotFound(ModalidadeNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(EquipeInativaException.class)
    public ResponseEntity<ApiError> handleEquipeInativa(EquipeInativaException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ModalidadeInativaException.class)
    public ResponseEntity<ApiError> handleModalidadeInativa(ModalidadeInativaException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InscricaoEquipeNotFoundException.class)
    public ResponseEntity<ApiError> handleInscricaoEquipeNotFound(InscricaoEquipeNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InscricaoAtletaNotFoundException.class)
    public ResponseEntity<ApiError> handleInscricaoAtletaNotFound(InscricaoAtletaNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InscricaoInvalidaException.class)
    public ResponseEntity<ApiError> handleInscricaoInvalida(InscricaoInvalidaException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AtletaForaDaCategoriaException.class)
    public ResponseEntity<ApiError> handleAtletaForaDaCategoria(AtletaForaDaCategoriaException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AtletaJaInscritoException.class)
    public ResponseEntity<ApiError> handleAtletaJaInscrito(AtletaJaInscritoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(EquipeJaInscritaException.class)
    public ResponseEntity<ApiError> handleEquipeJaInscrita(EquipeJaInscritaException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InscricaoEquipeSemAtletasException.class)
    public ResponseEntity<ApiError> handleInscricaoEquipeSemAtletas(InscricaoEquipeSemAtletasException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(UltimoAtletaAtivoException.class)
    public ResponseEntity<ApiError> handleUltimoAtletaAtivo(UltimoAtletaAtivoException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ResponseEntity<ApiError> handleTransicaoStatusInvalida(TransicaoStatusInvalidaException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(VinculoAtivoException.class)
    public ResponseEntity<ApiError> handleVinculoAtivo(VinculoAtivoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(NumeroCamisaJaUtilizadoException.class)
    public ResponseEntity<ApiError> handleNumeroCamisaJaUtilizado(NumeroCamisaJaUtilizadoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InscricoesEncerradasException.class)
    public ResponseEntity<ApiError> handleInscricoesEncerradas(InscricoesEncerradasException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(PeriodoInvalidoException.class)
    public ResponseEntity<ApiError> handlePeriodoInvalido(PeriodoInvalidoException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CompeticaoNaoEditavelException.class)
    public ResponseEntity<ApiError> handleCompeticaoNaoEditavel(CompeticaoNaoEditavelException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(TransicaoStatusCompeticaoException.class)
    public ResponseEntity<ApiError> handleTransicaoStatusCompeticao(TransicaoStatusCompeticaoException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(PartidaNotFoundException.class)
    public ResponseEntity<ApiError> handlePartidaNotFound(PartidaNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(PartidaInvalidaException.class)
    public ResponseEntity<ApiError> handlePartidaInvalida(PartidaInvalidaException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(TransicaoStatusPartidaException.class)
    public ResponseEntity<ApiError> handleTransicaoStatusPartida(TransicaoStatusPartidaException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldErrors> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldErrors(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDACAO, request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldErrors> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldErrors(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDACAO, request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, MSG_JSON_INVALIDO, request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, MSG_CONFLITO_DADOS, request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request, List<ApiError.FieldErrors> errors) {
        ApiError body = new ApiError(status.value(), message, LocalDateTime.now(), request.getRequestURI(), errors);
        return ResponseEntity.status(status).body(body);
    }
}
