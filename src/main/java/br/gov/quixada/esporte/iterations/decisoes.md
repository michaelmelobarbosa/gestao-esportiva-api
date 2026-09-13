# Decisões de Projeto — `GlobalExceptionHandler`

> Pasta `src/main/java/br/gov/quixada/esporte/iterations` — documento de estudo das decisões tomadas em `src/main/java/br/gov/quixada/esporte/common/error/GlobalExceptionHandler.java` e `ApiError.java`.
> Referência: ponto `7` `melhorias-atleta.md` `Tratamento de Erros`.

---

## 1. O que é `@RestControllerAdvice`

`GlobalExceptionHandler.java:16` anota a classe com `@RestControllerAdvice`.

* **Decisão:** centralizar o tratamento de exceções em uma única classe, em vez de `try/catch` espalhado em cada `Controller`.
* **Como funciona:** o Spring intercepta qualquer exceção lançada por controllers (`AtletaController.java`) e procura um método `@ExceptionHandler` compatível. O corpo retornado já vira o JSON da resposta HTTP.
* **Por que `@RestControllerAdvice` e não `@ControllerAdvice`:** `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`, então o `ApiError` é serializado automaticamente para JSON (sem precisar de `@ResponseBody` em cada método).
* **Escopo:** sem atributos (`basePackages`, `assignableTypes`), vale para **toda** a aplicação — adequado enquanto só existir o módulo `atleta`, mas revisar quando novos domínios surgirem.

---

## 2. O que é `ApiError` (o contrato de erro)

`ApiError.java:6`:

```java
public record ApiError(int status, String message, LocalDateTime timestamp, String path, List<ApiError.FieldErrors> errors) {
    public record FieldErrors(String field, String message) {}
}
```

* **Decisão:** usar `record` (Java 16+) em vez de classe com getters/setters. É imutável, conciso e serializado pelo Jackson direto pelo nome dos componentes.
* **`status`:** código HTTP (`400`, `404`, `409`) repetido dentro do corpo — ajuda clientes que só leem JSON e não o header HTTP.
* **`message`:** mensagem legível para o usuário.
* **`timestamp`:** quando ocorreu (`LocalDateTime.now()`). Útil para correlação em logs.
* **`path`:** URI que gerou o erro (`request.getRequestURI()`), ex.: `/v1/atletas`.
* **`errors`:** lista de erros por campo (validação). **Nunca `null`** — usamos `List.of()` quando não há erros (decisão de consistência, ver seção 6).
* **`record` aninhado `FieldErrors`:** permite `errors: [{ "field": "cpf", "message": "..." }]`. O nome está no plural por representar a lista conceitualmente, mas cada item é 1 erro.
* **Nota:** o guia menciona evoluir para `ProblemDetail` (RFC 7807). **Decisão deste projeto:** manter record próprio por simplicidade; migrar só se necessário interoperabilidade com ferramentas que consomem `application/problem+json`.

---

## 3. Método helper `build`

`GlobalExceptionHandler.java:66-70`:

```java
private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                       HttpServletRequest request, List<ApiError.FieldErrors> errors) {
    ApiError body = new ApiError(status.value(), message, LocalDateTime.now(), request.getRequestURI(), errors);
    return ResponseEntity.status(status).body(body);
}
```

* **Decisão:** extrair a montagem da resposta para **um só ponto**, evitando repetir `new ApiError(status.value(), msg, now(), uri, errors)` nos 7 handlers (princípio DRY).
* **Vantagem:** se `ApiError` ganhar um campo novo, muda-se apenas o helper.
* **`request.getRequestURI()`** dentro do helper garante que **todo** erro carregue `path`, não só o de validação.
* **`List.of()`** garante lista vazia imutável (nunca `null`).

---

## 4. Por que cada handler recebe `HttpServletRequest`

`GlobalExceptionHandler.java:23-64` — todos os métodos têm o parâmetro `HttpServletRequest request`.

* **Decisão:** obter a URI sem acoplar o código ao `Controller`.
* **Como:** Spring injeta o `HttpServletRequest` automaticamente no método do `@ExceptionHandler` — não é preciso passar nada manualmente.
* **Alternativa descartada:** `ServletUriComponentsBuilder.fromCurrentRequest()` (usada no `POST` `AtletaController.java:49`). Funciona, mas é global/estático; o `HttpServletRequest` é mais explícito e testável.

---

## 5. Handlers de exceção — cada decisão

| Handler | Exceção | HTTP | `message` | `errors` | Origem |
|---|---|---|---|---|---|
| `handleNotFound` `:23` | `AtletaNotFoundException` | `404 NOT_FOUND` | `ex.getMessage()` (amigável, nossa) | `[]` | `AtletaService.java:28` |
| `handleCpfCadastrado` `:28` | `CpfJaCadastradoException` | `409 CONFLICT` | `ex.getMessage()` | `[]` | `AtletaService.java:45` |
| `handleInativo` `:33` | `AtletaInativoException` | `409 CONFLICT` | `ex.getMessage()` | `[]` | `Atleta.java:90` (`atualizarDados`) |
| `handleConstraintViolation` `:38` | `ConstraintViolationException` | `400 BAD_REQUEST` | `"Erro de validação"` | por campo | `@Positive` em `@PathVariable` (`AtletaController.java:37,56,65`) |
| `handleMethodNotValid` `:47` | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `"Erro de validação"` | por campo | `@Valid @RequestBody` + Bean Validation nos DTOs |
| `handleHttpMessageNotReadable` `:56` | `HttpMessageNotReadableException` | `400 BAD_REQUEST` | `"JSON inválido"` | `[]` | JSON malformado / tipo errado no body |
| `handleDataIntegrity` `:61` | `DataIntegrityViolationException` | `409 CONFLICT` | `"Conflito de dados"` | `[]` | constraint do banco (ex.: `unique` de CPF) |

### Decisões de código de status

* **`404`** para recurso inexistente.
* **`409`** para conflito de estado/recursos (`CONFLICT`): CPF duplicado, atleta inativo, violação de integridade. Poderia ser `422 UNPROCESSABLE_ENTITY` para inativo, mas `409` foi mantido por simplicidade.
* **`400`** para erro do cliente: validação, JSON inválido.
* **Nunca `500`** para os casos acima: o handler "traduz" a exceção para o status correto.

### Decisão sobre `ex.getMessage()` vs mensagem fixa

* **Exceções de domínio** (`AtletaNotFound`, `CpfJaCadastrado`, `AtletaInativo`): usam `ex.getMessage()` porque **nós** escrevemos a mensagem (ex.: `"Atleta não encontrado"`), então é segura e útil.
* **Exceções técnicas** (validação, JSON, integridade): usam mensagem fixa porque o `getMessage()` do framework é longo e em inglês (ex.: `"Validation failed for argument [0]..."`), o que vaza detalhe interno e piora a UX.
* **Constantes** `MSG_VALIDACAO`/`MSG_JSON_INVALIDO`/`MSG_CONFLITO_DADOS` `:19-21`: centralizam textos e evitam string repetida.

---

## 6. `MethodArgumentNotValidException` — decisão detalhada

`GlobalExceptionHandler.java:47-54`:

```java
List<ApiError.FieldErrors> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> new ApiError.FieldErrors(fieldError.getField(), fieldError.getDefaultMessage()))
        .toList();
return build(HttpStatus.BAD_REQUEST, MSG_VALIDACAO, request, fieldErrors);
```

* **Quando ocorre:** quando `@Valid` (`AtletaController.java:44,57,66`) valida o `@RequestBody` e algum `@NotNull`/`@Past`/`@Pattern`/`@Size` do DTO falha. Ex.: `AtletaCreateRequest.java:21` `@Pattern cpf`.
* **`getBindingResult()`:** objeto que guarda o resultado da validação do DTO.
* **`getFieldErrors()`:** só os erros de **campo** (`cpf`, `dataNascimento`). Existem também os `getGlobalErrors()` (erros de classe, anotação no tipo todo) — **decisão:** ignorar global errors por simplicidade; nenhum DTO usa hoje.
* **`getField()`:** nome do campo com erro (ex.: `"cpf"`).
* **`getDefaultMessage()`:** a mensagem definida na anotação (ex.: `"CPF deve estar no formato 000.000.000-00 ou 00000000000"`).
* **`.toList()`:** criar lista imutável.
* **Resultado:** o frontend consegue destacar exatamente o campo inválido em vez de mostrar um texto único.

---

## 7. Diferença entre `MethodArgumentNotValidException` e `ConstraintViolationException`

Ambos viram `400`, mas vêm de lugares diferentes:

* **`MethodArgumentNotValidException`:** validação de **objeto** com `@Valid @RequestBody` → DTOs (records `AtletaCreateRequest`, `AtletaUpdateRequest`, `AtletaStatusRequest`). O erro tem `getBindingResult()`.
* **`ConstraintViolationException`:** validação **direta** de parâmetro com `@Validated` na classe (`AtletaController.java:22`) → anotações em `@PathVariable`/`@RequestParam`, ex.: `@Positive Long id` (`AtletaController.java:37,56,65`). O erro tem `getConstraintViolations()` com `getPropertyPath()` (ex.: `findById.id`).
* **Decisão:** tratar os dois separadamente e extrair `errors` em ambos (field + message), pois a estrutura de origem é diferente.

---

## 8. `DataIntegrityViolationException` — decisão detalhada

`GlobalExceptionHandler.java:61-64`.

* **Quando ocorre:** o banco rejeita uma operação por violar constraint/`FOREIGN KEY`/`NOT NULL`. Ex.: corrida de CPF duplicado (constraint `unique` em `Atleta.java:50`).
* **Cuidado/estado atual:** `AtletaService.java:43-47` **já captura** `DataIntegrityViolationException` no `save()` e converte para `CpfJaCadastradoException`, gerando mensagem melhor. Este handler é o **fallback genérico** para violações que escaparem dessa tradução.
* **Decisão de status:** `409 CONFLICT` por ser conflito com estado atual do banco.
* **Risco conhecido:** a mensagem fixa `"Conflito de dados"` esconde qual constraint falhou. Se quiser melhorar no futuro, inspecionar `ex.getMostSpecificCause().getMessage()` e mapear para mensagem amigável (não fazer agora — pode vazar nome de tabela/coluna).

---

## 9. Ordem e precedência dos handlers

* O Spring escolhe o handler **mais específico** pela classe da exceção, não pela ordem no arquivo. `AtletaNotFoundException` (nossa) tem precedência sobre `RuntimeException` genérica, se existisse.
* **Decisão:** não criar handler genérico `Exception` agora — mascararia bugs com `500` "bonito". Melhor deixar `500` padrão para erros inesperados (aparecem nos logs e são honestos em desenvolvimento).
* **Ponto de atenção:** `DataIntegrityViolationException` é bem genérica (cobre tudo relacionado a banco). Se outro domínio lançar a mesma exceção esperando tratamento diferente, será preciso refinar.

---

## 10. Fluxo completo de uma requisição inválida

Exemplo: `POST /v1/atletas` com body `{ "cpf": "1" }`.

1. `AtletaController.java:44` recebe `@Valid AtletaCreateRequest`.
2. Bean Validation roda `@NotBlank`/`@Pattern`/`@Size` (`AtletaCreateRequest.java:17-34`).
3. `@Pattern` do `cpf` falha → Spring lança `MethodArgumentNotValidException`.
4. `GlobalExceptionHandler.java:47` captura.
5. Extrai `fieldErrors` → `[{ field: "cpf", message: "CPF deve estar no formato..." }]`.
6. `build(...)` `:66` monta `ApiError` com `path=/v1/atletas`.
7. Resposta `400`:
```json
{
  "status": 400,
  "message": "Erro de validação",
  "timestamp": "2026-09-13T10:00:00",
  "path": "/v1/atletas",
  "errors": [{ "field": "cpf", "message": "CPF deve estar no formato..." }]
}
```
8. O `Controller` nem chega a executar `mapper.toEntity`/`service.save`.

---

## 11. Checklist de estudo

- [ ] Entender o ciclo `@Valid` → Bean Validation → exceção → `@ExceptionHandler` → JSON.
- [ ] Diferenciar `MethodArgumentNotValidException` (DTO) vs `ConstraintViolationException` (parâmetro).
- [ ] Entender por que os erros técnicos têm mensagem fixa e os de domínio usam `ex.getMessage()`.
- [ ] Saber por que `errors` nunca é `null` (`List.of()`).
- [ ] Entender o DRY do helper `build`.
- [ ] Estudar `ProblemDetail`/RFC 7807 e avaliar migração futura.
- [ ] Estudar `ResponseEntityExceptionHandler` como alternativa para sobrescrever erros padrão do Spring.

---

## 12. Referências

- Spring Boot — `@RestControllerAdvice`, `@ExceptionHandler`, `@Validated`
- Bean Validation (Jakarta) — `@Valid`, `ConstraintViolationException`
- Spring `BindingResult`, `FieldError`, `ObjectError`
- Spring DAO — `DataIntegrityViolationException`
- HTTP — RFC 9110 (status `400`/`404`/`409`)
- RFC 7807 — `ProblemDetail` (evolução futura)
- `melhorias-atleta.md` — ponto `7` Tratamento de Erros; ponto `6` validação `id`
