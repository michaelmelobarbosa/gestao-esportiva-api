# Análise e Melhorias — Módulo Atleta

> Gerado em 2026-09-10 | Projeto `gestao-esportiva-api` | Pacote `br.gov.quixada.esporte.atleta`
> Objetivo: guia de estudo didático para evoluir o código atual sem quebrar regras de negócio.

---

## Sumário
1. [Visão Geral](#1-visão-geral)
2. [Bugs e Erros Reais](#2-bugs-e-erros-reais--críticos)
3. [Falhas de Validação e Regra de Negócio](#3-falhas-de-validação-e-regra-de-negócio)
4. [Arquitetura e Design](#4-arquitetura-e-design)
5. [Performance e Persistência](#5-performance-e-persistência)
6. [API REST e Controller](#6-api-rest-e-controller)
7. [Tratamento de Erros](#7-tratamento-de-erros)
8. [Testabilidade e Qualidade](#8-testabilidade-e-qualidade)
9. [Infra e Configuração](#9-infra-e-configuração)
10. [Plano de Estudo Sugerido](#10-plano-de-estudo-sugerido)
11. [Checklist Prático](#11-checklist-prático)
12. [Referências](#12-referências)

---

## 1. Visão Geral

**Estrutura atual (correta):**
```
Atleta.java (Entity) 
  <- AtletaRepository.java (JpaRepository)
  <- AtletaService.java (Regra de negócio + @Transactional)
  <- AtletaController.java (REST /v1/atletas)
  <- AtletaMapper.java (MapStruct)
  <- dto/AtletaCreateRequest.java, dto/AtletaUpdateRequest.java, dto/AtletaResponse.java, dto/AtletaResumoResponse.java
  <- extras/Endereco.java, CpfUtils.java, Sexo.java, StatusAtleta.java
  <- exceptions/GlobalExceptionHandler.java
```

Pontos fortes: separação em camadas, uso de `record` para DTOs, MapStruct para `idade` calculada, tratamento de `DataIntegrityViolationException` para corrida de CPF.

Oportunidades: validação duplicada/conflitante, falta de paginação, handler de erros incompleto, exposição de `setter` na entidade, código morto.

---

## 2. Bugs e Erros Reais — Críticos

### 2.1 `Atleta.java:54-56` — Validação conflitante de CPF ✅ Corrigido
```java
@CPF(message = "CPF inválido")
@Pattern(regexp = "\\d{11}", message = "CPF deve conter apenas números e ter 11 dígitos")
private String cpf;
```
- `@CPF` (hibernate-validator) **aceita** `529.982.247-25` formatado.
- `@Pattern("\\d{11}")` **rejeita** qualquer máscara antes de `normalizarCpf()` (`Atleta.java:84`) ser chamado (`@PrePersist/@PreUpdate` roda depois da validação).

**Efeito:** `POST /v1/atletas` com `"cpf":"529.982.247-25"` retorna `400` mesmo que `CpfUtils.normalize()` (`extras/CpfUtils.java:8`) resolveria.

**Estudar:** Bean Validation lifecycle (`@Valid` -> `@PrePersist`), ordem de interceptors JPA.
**Correção:**
- Remover `@CPF`/`@Pattern` da entidade. Deixar validação só nos DTOs.
- Ou trocar Pattern para aceitar ambos: `"(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})"`.
- Garantir `normalize()` no Service (`AtletaService.java:40`) *antes* de `existsByCpf()`.

### 2.2 `Atleta.java:78` — `@CreationTimestamp` Hibernate-específico ✅ Corrigido
```java
@CreationTimestamp
@Column(nullable = false, updatable = false)
private LocalDateTime dataCadastro;
```
Funciona, mas amarra ao Hibernate. Quebra se trocar de provider JPA e dificulta teste unitário (precisa subir contexto).

**Estudar:** `@CreationTimestamp` vs `@CreatedDate` (Spring Data Auditing) vs `@PrePersist`.
**Correção:**
```java
@Column(nullable = false, updatable = false)
private LocalDateTime dataCadastro;

@PrePersist
void prePersist() {
    this.dataCadastro = LocalDateTime.now();
    normalizarCpf();
}
```

### 2.3 `Atleta.java:82` — `public void normalizarCpf()` ✅ Corrigido
```java
@PrePersist @PreUpdate
public void normalizarCpf() { this.cpf = CpfUtils.normalize(this.cpf); }
```
Deveria ser `private`/`protected`. `public` expõe método interno. E é chamado em `@PreUpdate` mesmo quando `cpf == null` (caso `AtletaMapper.java:28` mapear `AtletaUpdateRequest` sem cpf) — funciona porque `CpfUtils.normalize(null)==null`, mas esconde intenção.

**Correção:** `private void normalizarCpf()` + `if (this.cpf != null)`.

### 2.4 `AtletaRepository.java:16` — Busca case-sensitive e sem limite ✅ Corrigido
```java
List<Atleta> findByNomeCompletoContaining(String name);
```
- `Containing` gera `LIKE %name%` (case-sensitive dependendo do collation MySQL).
- Sem `Pageable` traz **tudo** para memória (`AtletaService.java:22`).

**Correção:** `Page<Atleta> findByNomeCompletoContainingIgnoreCase(String name, Pageable pageable);`

### 2.5 `AtletaController.java:50` vs `57` — Nomenclatura inconsistente ✅ Corrigido
`@PatchMapping("/{id}/inativar")` com método `desativarById()` chamando `service.inativar()`. Escolha um verbo e mantenha (`inativar`/`desativar`).

### 2.6 `AtletaController.java:40` — Falta `Location` header ✅ Corrigido
REST padrão: `POST` deve retornar `201` + `Location: /v1/atletas/{id}`.

**Correção:**
```java
URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(saved.getId()).toUri();
return ResponseEntity.created(location).body(mapper.toGetResponse(saved));
```

---

## 3. Falhas de Validação e Regra de Negócio

### 3.1 `dto/AtletaCreateRequest.java:22` e `dto/AtletaUpdateRequest.java:16` — Falta `@Past` ✅ Corrigido
Entidade tem `@Past` (`Atleta.java:59`), mas DTO não. Cliente envia `dataNascimento: 2090-01-01` e só falha na persistência (vira `500` se bypass).

**Correção DTO:**
```java
@NotNull @Past(message = "Data de nascimento deve ser no passado")
LocalDate dataNascimento,
```

### 3.2 `dto/AtletaCreateRequest.java:20` — `@Size(max=14)` permissivo ✅ Corrigido
```java
@CPF @Size(max=14) String cpf,
```
Permite `"1"`. Se aceitar máscara, precisa `@Size(min=11, max=14)` ou melhor `@Pattern` explícito. Como `CpfUtils.normalize()` converte para 11, valide no Service após normalizar.

### 3.3 `extras/EnderecoRequest.java:21` — Redundância ✅ Corrigido
```java
@Pattern(regexp="\\d{5}-\\d{3}") @Size(max=10) String cep,
```
`@Pattern` já garante 9 chars (`12345-678`), `@Size` é redundante. E `@NotBlank` já implica `!= null`.

### 3.4 `AtletaService.java:39` — Corrida de CPF ✅ Corrigido
`existsByCpf()` + `save()` tem race condition (TOCTOU). Você já trata com `catch (DataIntegrityViolationException)` — **ótimo**. Pode simplificar removendo `existsByCpf()` e confiando só na constraint `unique` (`Atleta.java:53`) + catch.

### 3.5 `AtletaService.java:63-75` — `ativar()/inativar()` redundante ✅ Corrigido
Dentro de `@Transactional`, `repository.save(atleta)` é desnecessário — JPA dirty checking já persiste. E não verifica idempotência (ativar já ativo).

**Melhoria:**
```java
@Transactional
public Atleta inativar(Long id) {
    Atleta a = findByIdOrThrowNotFound(id);
    if (a.getStatus() == StatusAtleta.INATIVO) return a;
    a.setStatus(StatusAtleta.INATIVO);
    return a; // sem save
}
```

### 3.6 `AtletaService.java:78` — `update()` silencioso sobre CPF ✅ Corrigido
```java
atletaExistente.setNomeCompleto(...); // 5 campos
// cpf não copiado -> imutável (correto, mas não documentado)
```
Bom que CPF não muda via `AtletaUpdateRequest` (DTO sem cpf), mas documente regra: “CPF imutável após criação”. E considere bloquear update se `status == INATIVO`.

**Correção aplicada (3.6):**
- `AtletaService.java:90` `if (status == INATIVO) throw new AtletaInativoException(...)` + `GlobalExceptionHandler.java:24` `409 CONFLICT`
- `AtletaUpdateRequest.java:13` sem campo `cpf` + `AtletaService.java:94-100` sem `setCpf()` => regra `CPF imutável após criação` documentada via `update()` + `return atletaExistente` sem `save()` (dirty checking `3.5`)
- Limpeza `Logger` não usado `AtletaService.java:4-5` removido + javadoc `/** CPF imutável após criação; bloqueia edição se INATIVO */` em `update()`

---

## 4. Arquitetura e Design

### 4.1 `Atleta.java:36` — `@Setter` na entidade
Expõe `setId()`, `setDataCadastro()`, `setCpf()` para qualquer código. Entidade JPA deve ter mutação controlada.

**Estudar:** DDD, anemic model vs rich model.
**Correção:** `@Getter` apenas + métodos de negócio:
```java
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Atleta {
    @Setter(AccessLevel.PACKAGE) private Long id; // ou sem setter
    public void inativar(){ this.status = StatusAtleta.INATIVO; }
}
```

### 4.2 `AtletaMapper.java:42` — `LocalDate.now()` não testável
```java
default Integer calcularIdade(LocalDate data) {
    return Period.between(data, LocalDate.now()).getYears();
}
```
`LocalDate.now()` depende de relógio do sistema e timezone. Teste falha amanhã.

**Correção:** Injetar `Clock`:
```java
@Mapper(componentModel="spring")
public interface AtletaMapper {
    @Mapping(target="idade", expression="java(calcularIdade(atleta.getDataNascimento(), clock))")
    AtletaResponse toGetResponse(Atleta atleta, @Context Clock clock);
    default Integer calcularIdade(LocalDate d, Clock c){ return d==null?null:Period.between(d, LocalDate.now(c)).getYears(); }
}
```
No Service/Controller: `mapper.toGetResponse(atleta, clock)`.

### 4.3 `AtletaMapper.java:23` — `toEntity` ignora `cpf` implicitamente
Para `AtletaUpdateRequest`, `cpf` nem existe no record, então MapStruct não mapeia — parece “ignorar”, mas é acidental. Seja explícito:
```java
@Mapping(target="cpf", ignore=true)
@Mapping(target="id", ignore=true)
Atleta toEntity(AtletaUpdateRequest request);
```

### 4.4 `AtletaService.java:33` — Código morto `findByCpfOrThrowNotFound`
Nunca usado no Controller. Ou exponha `GET /v1/atletas/cpf/{cpf}` ou remova para não poluir API.

---

## 5. Performance e Persistência

| Problema | Impacto | Solução |
|---|---|---|
| `findAll(String)` sem `Pageable` (`AtletaService.java:22`) | OOM com 10k+ registros | `Page<Atleta> findAll(Pageable)` |
| `findByNomeCompletoContaining` sem índice | Full table scan `LIKE %x%` | Adicionar `Pageable` + `IgnoreCase` + índice MySQL `FULLTEXT` se busca por nome for frequente |
| `repository.findAll()` no Controller (`AtletaController.java:26`) | Serializa `List<AtletaResumoResponse>` inteira | Paginar e retornar `Page<AtletaResumoResponse>` |
| `ddl-auto:update` (`application.yaml:12`) | Perigoso em prod, pode dropar coluna | `ddl-auto: validate` + Flyway/Liquibase |

**Estudar:** Paginação Spring Data, `PageRequest`, `Sort`, índices MySQL, `EXPLAIN`.

---

## 6. API REST e Controller

- **Versionamento:** `/v1/atletas` OK, mas documente estratégia (URL vs header).
- **Verbos:** `PUT /{id}` para `update` (`AtletaController.java:65`) exige payload completo (correto). `PATCH /{id}/ativar` e `/inativar` poderiam ser `PATCH /{id}/status` com body `{ "status":"INATIVO" }` — mais RESTful.
- **Validação `id`:** Falta `@Positive` ou `@NotNull` em `@PathVariable Long id`. Enviar `id=-1` cai em `404` genérico.
- **Filtro:** `GET /v1/atletas?nome=joao` aceita `nome=null` vs `nome=""` — trate `isBlank()` no Service.
- **Segurança:** Sem `@PreAuthorize`. Comentário em `AtletaService.java:55` indica `hardDelete` para admin — planeje roles.

---

## 7. Tratamento de Erros

`GlobalExceptionHandler.java:11` só trata 2 exceções.

**Faltam:**
- `MethodArgumentNotValidException` (falha `@Valid` nos DTOs) -> `400` com lista `fieldErrors`.
- `ConstraintViolationException`
- `HttpMessageNotReadableException` (JSON malformado)
- `DataIntegrityViolationException` genérico

**Modelo atual `ApiError.java:5`:**
```java
public record ApiError(int status, String message, LocalDateTime timestamp){}
```
Sugestão evoluir para RFC 7807 (`ProblemDetail` do Spring 6):
```java
public record ApiError(int status, String message, LocalDateTime timestamp, String path, List<FieldError> errors){}
```

**Estudar:** `@RestControllerAdvice`, `ResponseEntityExceptionHandler`, `ProblemDetail`.

---

## 8. Testabilidade e Qualidade

- **Sem testes:** `src/test/**/*Atleta*` vazio. Crie:
  - `AtletaServiceTest` (mock `AtletaRepository`, testa `save` com CPF duplicado, `ativar/inativar`)
  - `AtletaControllerTest` com `@WebMvcTest` + `MockMvc`
  - `AtletaMapperTest` com `Clock` fixo (`Clock.fixed(...)`)
- **Lombok + JPA:** `@EqualsAndHashCode(onlyExplicitlyIncluded=true)` com `id` é OK, mas dois objetos `transient` (`id==null`) nunca são iguais — documente e evite usar em `Set` antes de persistir.
- **Telefone:** `Atleta.java:68` `String telefone` sem `@Pattern`. Valide formato `(88) 9xxxx-xxxx` ou normalize como CPF.

---

## 9. Infra e Configuração

`pom.xml:16` `java.version=25` + `spring-boot-starter-parent:4.1.0` — combinação muito recente (2026). Verifique compatibilidade MapStruct `1.6.3` com Java 25.

`application.yaml:13` `show-sql:true` + `format_sql:true` — ótimo dev, desative em prod (`logging.level.org.hibernate.SQL=DEBUG`).

Falta `spring.jpa.open-in-view=false` para evitar `LazyInitializationException` e N+1.

Falta documentação OpenAPI: adicione `springdoc-openapi-starter-webmvc-ui`.

---

## 10. Plano de Estudo Sugerido

Estude na ordem (do menor risco ao maior impacto):

**Semana 1 — Validação:**
1. Leia Bean Validation (JSR 380) e ciclo JPA `@PrePersist`.
2. Corrija `@Past` nos DTOs e remova `@Pattern` da entidade.
3. Teste manualmente `POST` com `cpf` formatado vs só números.

**Semana 2 — API:**
1. Estude paginação (`Pageable`, `Page<T>`) e implemente `GET /v1/atletas?page=0&size=20&nome=joao`.
2. Adicione `Location` no `POST`.
3. Unifique `ativar/inativar` e valide `@Positive`.

**Semana 3 — Erros:**
1. Estude `@RestControllerAdvice` e `ProblemDetail`.
2. Implemente handler para `MethodArgumentNotValidException` retornando `ApiError` com `errors`.
3. Teste com `curl` enviando JSON inválido.

**Semana 4 — Qualidade:**
1. Estude `Clock` e testabilidade.
2. Refatore `calcularIdade()` e `Atleta` setters.
3. Escreva `AtletaServiceTest` e `AtletaControllerTest` (cobertura >80%).
4. Troque `ddl-auto:update` por Flyway.

---

## 11. Checklist Prático

- [x] Remover `@CPF/@Pattern` de `Atleta.java:54` ou ajustar regex (2.1 — feito: entidade só `@Pattern(\d{11})`, DTO aceita ambos os formatos)
- [x] Tornar `normalizarCpf()` privado + null-safe (2.1/2.3 — feito: `private`, null-safe via `CpfUtils`)
- [x] Adicionar `@Past` em `AtletaCreateRequest.java:22` e `AtletaUpdateRequest.java:16` (3.1 — feito: `@Past + @NotNull` em `AtletaCreateRequest.java:24` e `AtletaUpdateRequest.java:17`)
- [x] Corrigir `@Size` do CPF para `min=11` (3.2 — feito: `@Pattern` + `@Size(min=11,max=14)` em `AtletaCreateRequest.java:21-22`, DTO aceita `000.000.000-00` ou `00000000000`)
- [x] Criar `Page<Atleta> findByNomeCompletoContainingIgnoreCase(...)` + paginar Controller (2.4 — feito: `Page<Atleta> + Pageable + IgnoreCase` em `AtletaRepository.java:18`, `Page.map` em `AtletaController.java:37`)
- [x] Tratar `nome.isBlank()` em `AtletaService.java:22` (2.4 — feito: `name==null || name.isBlank()` em `AtletaService.java:28`)
- [x] Remover `repository.save()` redundante em `ativar/inativar` (3.5 — feito: `ativar`/`inativar`/`update` sem `save()` via dirty checking + idempotência em `AtletaService.java:58-93`)
- [x] Padronizar `inativar` vs `desativar` (2.5 — feito: `inativarById`/`ativarById` em `AtletaController.java:53,61` padronizado com `service.inativar/ativar`)
- [x] Adicionar `Location` no `POST` (2.6 — feito: `ResponseEntity.created(location)` em `AtletaController.java:50` com `ServletUriComponentsBuilder`)
- [ ] Implementar handlers para `MethodArgumentNotValidException` etc.
- [ ] Evoluir `ApiError.java:5` para incluir `path` e `errors`
- [ ] Injetar `Clock` no `AtletaMapper.java:42`
- [ ] Adicionar `cpf` ignore explícito no `toEntity(UpdateRequest)`
- [ ] Remover ou expor `findByCpfOrThrowNotFound`
- [ ] Trocar `@Setter` da entidade por métodos de domínio
- [ ] Adicionar testes (`AtletaServiceTest`, `AtletaControllerTest`)
- [ ] `application.yaml:12` -> `validate` + Flyway
- [ ] `open-in-view=false` + OpenAPI

---

## 12. Referências

- Bean Validation 3.0 (Jakarta) — `@CPF`, `@Past`, `@Valid`
- Spring Data JPA — `JpaRepository`, `Pageable`, `ContainingIgnoreCase`
- MapStruct 1.6.3 docs — `componentModel="spring"`, `@Context`, `expression`
- Spring Boot 3.x/4.x — `@RestControllerAdvice`, `ProblemDetail` (RFC 7807)
- Hibernate — `@CreationTimestamp`, `@PrePersist`, dirty checking
- MySQL — `LIKE %x%` vs `FULLTEXT`, collation `utf8mb4_unicode_ci`
- REST — `201 Created` + `Location`, idempotência `PUT` vs `PATCH`

> Dica: cada item acima pode virar um commit pequeno. Abra um branch `estudos/atleta-melhorias` e faça PRs atômicos — facilita revisão e aprendizado.

