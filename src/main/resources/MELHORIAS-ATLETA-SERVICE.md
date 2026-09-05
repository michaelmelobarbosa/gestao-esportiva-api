# Pontos de Melhoria — `AtletaService`

Análise gerada a partir de `AtletaService.java`, `Atleta.java`, `AtletaRepository.java`, `AtletaMapper.java`, DTOs (`AtletaPostRequest`, `AtletaPutRequest`, `AtletaGetResponse`), `InscricaoAtleta.java`, `Endereco.java` e exceções (`AtletaNotFoundException`, `CpfJaCadastradoException`).

---

## 1. [CRÍTICO] `Endereco` sem Lombok (bug)

**Onde:** `extras/Endereco.java:10`

**Problema:**
Classe `@Embeddable` sem `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`.
Isso quebra:
- `AtletaService.java:56` → `atletaExistente.setEndereco(...)`
- `AtletaMapper.java:34-36` → `toEndereco()` / `toEnderecoResponse()` (MapStruct não gera acesso)
- Desserialização Jackson de `EnderecoRequest`.

**Sugestão:**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class Endereco { ... }
```

---

## 2. [ALTO] Incoerência hard-delete vs. soft-delete

**Onde:** `AtletaService.java:35-47`, `Atleta.java:46`

**Problema:**
- `Atleta.ativo` + `save()` forçando `setAtivo(true)` sugere soft-delete.
- Mas `delete()` faz `repository.delete(atleta)` (hard-delete físico).
- `InscricaoAtleta.java:26-28` tem FK `nullable=false` para `Atleta`. Deletar atleta com inscrição causa `DataIntegrityViolationException` (500 não tratado).

**Sugestões:**
- Opção A (recomendada se há histórico esportivo): soft-delete:
  ```java
  @Transactional
  public void delete(Long id) {
      Atleta atleta = findByIdOrThrowNotFound(id);
      atleta.setAtivo(false);
  }
  ```
  + criar `ativar(Long id)` / `inativar(Long id)` explícitos.
  + filtrar `findAll()` por `ativo=true` ou expor parâmetro (`findAllAtivos()`, `findAll(Pageable)`).
- Opção B (se hard-delete é intencional): remover campo `ativo` da entidade, DTOs e mapper para não confundir.
- Em ambos os casos: tratar `DataIntegrityViolationException` no delete e/ou verificar `exists InscricaoAtleta by atleta` antes, retornando 409 com mensagem de negócio (“atleta possui inscrições”).

---

## 3. [ALTO] `save()` / `update()` recebem entidade em vez de DTO

**Onde:** `AtletaService.java:35,50`, `AtletaMapper.java:21,27`

**Problema:**
- Assinaturas `save(Atleta)` e `update(Long, Atleta)` expõem a camada de persistência para o controller.
- `AtletaMapper.updateEntity(AtletaPutRequest)` nunca é chamado (código morto).
- `update()` faz `set` manual campo a campo (`AtletaService.java:53-57`), duplicando a lógica do MapStruct. Todo campo novo precisa lembrar de atualizar em dois lugares.
- Permite `null overwrite`: se `atletaParaAtualizar.getEndereco()` vier `null`, sobrescreve com `null` e estoura `ConstraintViolation` no flush.
- Pula Bean Validation (service aceitando entidade não garante `@Valid`).

**Sugestão:**
Mudar service para trabalhar com DTOs e usar `@MappingTarget`:
```java
@Mapper(componentModel = "spring")
public interface AtletaMapper {
    Atleta toEntity(AtletaPostRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cpf", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    void updateEntity(AtletaPutRequest dto, @MappingTarget Atleta entity);
}
```
```java
@Transactional
public Atleta save(AtletaPostRequest dto) {
    // validar CPF, normalizar, mapear, salvar
}

@Transactional
public Atleta update(Long id, AtletaPutRequest dto) {
    Atleta existente = findByIdOrThrowNotFound(id);
    mapper.updateEntity(dto, existente);
    return existente; // dirty-checking, sem save() explícito
}
```

---

## 4. [ALTO] CPF: race condition + falta de normalização

**Onde:** `AtletaService.java:36-38`, `Atleta.java:31-33`, `AtletaRepository.java:11`

**Problema:**
- Padrão `existsByCpf()` + `save()` tem TOCTOU: sob concorrência, dois requests passam no `exists` e um falha com `Duplicate entry` (500) em vez de 409.
- Sem normalização: `@CPF` aceita com e sem máscara (`12345678909` vs `123.456.789-09`), mas `existsByCpf` compara string literal. Mesmo CPF em formatos diferentes é cadastrado duas vezes.
- `@Size(max=14)` + `@Column(length=14)` conflita com CPF sem máscara (11 chars — ok) vs com máscara (14 chars — ok), mas não garante um padrão único.
- `findByCpfOrThrowNotFound()` tem o mesmo problema de formato.

**Sugestões:**
1. Definir padrão único (ex.: só dígitos) e normalizar na entrada:
   ```java
   String cpfNormalizado = atleta.getCpf().replaceAll("\\D", "");
   ```
   Aplicar em `save()` e `findByCpf()`.
2. Manter `unique=true` no banco como garantia final e converter a exceção:
   ```java
   try {
       return repository.save(atleta);
   } catch (DataIntegrityViolationException e) {
       throw new CpfJaCadastradoException("CPF já cadastrado");
   }
   ```
3. Opcional: criar método `existsByCpfNormalized` ou guardar sempre normalizado.

---

## 5. [MÉDIO] `findAll()` sem paginação + exposição de entidade

**Onde:** `AtletaService.java:17-20`

**Problema:**
- `List<Atleta> findAll()` sem `Pageable` não escala (traz tabela inteira para memória).
- Retornar entidade `Atleta` vaza detalhes de persistência (proxies LAZY, `dataCadastro`), dificulta versionamento da API.

**Sugestão:**
```java
@Transactional(readOnly = true)
public Page<Atleta> findAll(Pageable pageable) {
    return repository.findAll(pageable);
}
// ou
public Page<AtletaGetResponse> findAll(Pageable pageable) {
    return repository.findAll(pageable).map(mapper::toGetResponse);
}
```
Se o service continuar retornando entidade, deixar o controller mapear para DTO. Definir um padrão único para o projeto (service→DTO é o mais comum com MapStruct).

---

## 6. [MÉDIO] Exceções de domínio acopladas a HTTP

**Onde:** `exceptions/AtletaNotFoundException.java:6`, `exceptions/CpfJaCadastradoException.java:6`

**Problema:**
Estender `ResponseStatusException` amarra a camada de serviço ao Spring-Web. Dificulta reuso (jobs, testes sem contexto web) e Padronização de erro (RFC 7807 / `ProblemDetail`).

**Sugestão:**
```java
public class AtletaNotFoundException extends RuntimeException { ... }
public class CpfJaCadastradoException extends RuntimeException { ... }

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AtletaNotFoundException.class)
    ProblemDetail handleNotFound(...) { ... 404 ... }

    @ExceptionHandler(CpfJaCadastradoException.class)
    ProblemDetail handleConflict(...) { ... 409 ... }
}
```

---

## 7. [BAIXO] `repository.save()` redundante no `update()`

**Onde:** `AtletaService.java:59`

**Problema:**
Dentro de `@Transactional`, a entidade retornada por `findByIdOrThrowNotFound()` está gerenciada. O dirty-checking do JPA já faz o `UPDATE` no commit. O `save()` explícito é inofensivo, mas redundante.

**Sugestão:** remover `return repository.save(...)` e apenas `return atletaExistente;` (após `mapper.updateEntity(...)`). Manter `save()` apenas no `save()` / `insert`.

---

## 8. [BAIXO] `ativo` como `boolean` primitivo + `columnDefinition`

**Onde:** `Atleta.java:45-46`

**Problema:**
- `boolean` primitivo sempre default `false` no `new Atleta()` / `@Builder`, exigindo `setAtivo(true)` manual no service (esquecimento fácil).
- `columnDefinition = "BOOLEAN DEFAULT TRUE"` é específico de banco e pode falhar em H2/Postgres/MySQL de formas diferentes.

**Sugestão:**
```java
@Column(nullable = false)
@Builder.Default
private Boolean ativo = true;

@PrePersist
void prePersist() {
    if (ativo == null) ativo = true;
}
```
E no mapper manter `@Mapping(target = "ativo", ignore = true)`.

---

## 9. [BAIXO] Validações de negócio ausentes

**Onde:** `AtletaPostRequest.java`, `AtletaPutRequest.java`, `Atleta.java:36`

**Problema:**
- `dataNascimento` aceita data futura (falta `@Past`).
- Sem idade mínima (ex.: atleta mirim?) — regra de negócio não expressa.
- `telefone` só tem `@Size(max=20)`, sem padrão (aceita qualquer string).

**Sugestão:**
```java
@NotNull @Past(message = "Data de nascimento deve estar no passado")
LocalDate dataNascimento,
```
+ validar idade mínima no service se houver regra (ex.: `Period.between(...)`).
+ `@Pattern` para telefone se houver formato padrão municipal.

---

## 10. [BAIXO] `findByCpfOrThrowNotFound` possivelmente não usado + detalhe transacional

**Onde:** `AtletaService.java:28-32,43-51`

**Problema:**
- Nenhum controller ainda usa o service (busca não encontrou `AtletaController`). Se `findByCpf` não terá endpoint, é código a manter sem uso.
- `delete()` / `update()` chamam `findByIdOrThrowNotFound()` (que é `@Transactional(readOnly=true)`) dentro de transação de escrita — funciona por propagação `REQUIRED`, mas o `readOnly` interno é ignorado. Não é bug, só detalhe.

**Sugestão:**
- Manter `findByCpf` apenas se houver endpoint `GET /atletas?cpf=` ou `GET /atletas/cpf/{cpf}` planejado; caso contrário, remover até precisar (YAGNI).
- Opcional micro-otimização em `delete()`: usar `getReferenceById()` + `delete()` para evitar um `SELECT`, mas o jeito atual (buscar para validar 404) está correto e mais legível.

---

## Resumo de prioridade

| Prioridade | Item |
|---|---|
| Crítico | 1. Lombok em `Endereco` |
| Alto | 2. Soft vs hard delete + FK inscrição |
| Alto | 3. Service receber DTO + `@MappingTarget` |
| Alto | 4. Normalizar CPF + tratar `DataIntegrityViolationException` |
| Médio | 5. Paginação + não expor entidade |
| Médio | 6. Desacoplar exceções de HTTP (`@ControllerAdvice`) |
| Baixo | 7–10. `save()` redundante, `ativo` wrapper, `@Past`, YAGNI `findByCpf` |
