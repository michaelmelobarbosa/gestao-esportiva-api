# Futuras Iterações — AtletaService

> Pasta `src/main/java/br/gov/quixada/esporte/iterations` — lembretes para não esquecer de voltar e implementar métodos comentados em `src/main/java/br/gov/quixada/esporte/atleta/AtletaService.java:32` e `:50`.

---

## 1. `hardDelete(Long id)` — `AtletaService.java:50-55` comentado

```java
// hard delete a ser implementado quando perfil admin for criado
// @Transactional
// public void hardDelete(Long id) {
//     Atleta atleta = findByIdOrThrowNotFound(id);
//     repository.delete(atleta);
// }
```

**Por que está comentado:** `AtletaService.java:50` indica que `hardDelete` só deve existir com perfil `admin`. Hoje o `inativar()` `AtletaService.java:64` é soft delete (status INATIVO). Hard delete físico é perigoso sem controle de permissão.

**Para implementar no futuro:**
- [ ] Criar role `ADMIN` + `@PreAuthorize("hasRole('ADMIN')")` em `AtletaController.java:19`
- [ ] Adicionar endpoint `AtletaController.java:19`:
  ```java
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> hardDelete(@PathVariable Long id) {
      service.hardDelete(id);
      return ResponseEntity.noContent().build();
  }
  ```
- [ ] Descomentar `AtletaService.java:50` e validar regra: bloquear `hardDelete` se `atleta.status == ATIVO` (?) ou permitir sempre?
- [ ] Considerar `ON DELETE` em `db/migration` para tabelas relacionadas (`inscricao`, `equipe`) — `Atleta.java:37` `@Table(name="db_atletas")`
- [ ] Teste: `AtletaServiceTest#hardDelete_deveRemoverQuandoAdmin`

**Nota:** `AtletaService.java:33` código morto — ou expor `GET /cpf` ou remover.

---

## 2. `findByCpfOrThrowNotFound(String cpf)` — `AtletaService.java:32-36` comentado

```java
//    @Transactional(readOnly = true)
//    public Atleta findByCpfOrThrowNotFound(String cpf) {
//        return repository.findByCpf(CpfUtils.normalize(cpf))
//                .orElseThrow(() -> new AtletaNotFoundException("Atleta não encontrado"));
//    }
```

**Por que está comentado:** Método já correto (usa `CpfUtils.java:8` `normalize` + `AtletaRepository.java:18` `findByCpf`), mas `AtletaController.java:19` nunca expõe `GET /v1/atletas/cpf/{cpf}` — fica código morto.

**Para implementar no futuro:**
- [ ] Descomentar `AtletaService.java:32`
- [ ] Expor em `AtletaController.java:19` (decidir URL para não colidir com `GET /{id}`):
  ```java
  @GetMapping("/cpf/{cpf}")
  public ResponseEntity<AtletaResponse> findByCpf(@PathVariable String cpf) {
      Atleta atleta = service.findByCpfOrThrowNotFound(cpf);
      return ResponseEntity.ok(mapper.toGetResponse(atleta, clock));
  }
  // ou @GetMapping(params="cpf") -> GET /v1/atletas?cpf=52998224725
  ```
  Atenção: `@GetMapping("/{id}")` `AtletaController.java:34` já captura `/{cpf}` se `cpf` for numérico — usar `/cpf/{cpf}` evita ambiguidade.
- [ ] Validar `cpf` com `@CPF`/`@Pattern` no `@PathVariable` + `CpfUtils.normalize` já feito no service
- [ ] Adicionar `@ExceptionHandler` em `GlobalExceptionHandler.java:11` já cobre `AtletaNotFoundException` -> `404`
- [ ] Índice: `Atleta.java:45` `@Column(unique=true)` já garante busca por `cpf` eficiente; considerar `findByCpf` já usa índice único
- [ ] Teste: `AtletaControllerTest#findByCpf_deveRetornar200QuandoExistirComMascara` (testar `529.982.247-25` e `52998224725`)

---

## 3. Busca por nome com índice — `AtletaService.java:22` / `AtletaRepository.java` ✅ Feito (opção D1)

> `findByNomeCompletoContaining` sem índice → `FULL TABLE SCAN` com `LIKE %x%`.

**Status:** ✅ Resolvido com **coluna normalizada + índice BTREE + busca por prefixo** (opção D1). O `FULLTEXT` foi avaliado e descartado.

**O que foi feito:**
- [x] `extras/NomeCompletoUtils.java`: normaliza o nome — NFD + `\p{M}` (remove acento), `[^A-Za-z]` (remove símbolos/espaços), `toLowerCase(Locale.ROOT)`.
- [x] `Atleta.java`: coluna `nome_completo_normalizado` (`nullable = false`), preenchida em `@PrePersist`/`@PreUpdate` e no `service.save`.
- [x] `AtletaRepository.java`: `findByNomeCompletoNormalizadoStartingWith` (gera `LIKE 'joao%'`, usa índice).
- [x] `AtletaService.java:22`: normaliza a **entrada** da busca com a mesma função (simetria coluna × input).
- [x] Migration `V10__atleta_nomecompleto_normalizado.sql`: cria a coluna + `idx_atleta_nome_completo_normalizado`.
- [x] Testes: `AtletaServiceTest` (busca normalizada), `NomeCompletoUtilsTest` (regra de normalização) e `AtletaRepositoryTest` (`@DataJpaTest`, cobre `@PrePersist`/`@PreUpdate`).

**Decisão / tradeoff:** a busca passou a ser por **prefixo** — só acha quem começa pelo termo. `FULLTEXT`/`MATCH AGAINST` foi descartado por mudar a semântica (busca por palavra, não substring), exigir query nativa + `countQuery` e ter regras próprias (tokens curtos, stopwords). Se um dia for necessário buscar no meio do nome, reavaliar `FULLTEXT` com parser `ngram`.

**Ressalva do `V10`:** sem backfill — aplicado num banco recriado (vazio). Se algum dia rodar sobre tabela com dados, precisa de backfill, porque a normalização Java (remove acento/espaço) não é reproduzível por um `lower()` simples em SQL.

---

## 4. `ddl-auto:update` → `validate` + Flyway/Liquibase — `application.yaml` ✅ Feito

> `ddl-auto:update` perigoso em prod (pode dropar coluna).

**Status:** ✅ Adotado o Flyway. Baseline criado a partir do DDL gerado pelo Hibernate e banco de dev recriado limpo. `contextLoads` aplica `V1` e valida o schema.

**O que foi feito:**
- [x] Dependências no `pom.xml`: `spring-boot-starter-flyway` (auto-config — no Spring Boot 4 o `flyway-mysql` sozinho **não** ativa o Flyway) + `flyway-mysql`
- [x] `V1__baseline.sql` em `src/main/resources/db/migration/` gerado via `jakarta.persistence.schema-generation.scripts.action=create` (DDL idêntico ao esperado pelo Hibernate)
- [x] `application.yaml`: `ddl-auto: validate` + bloco `spring.flyway` (`enabled`, `locations`, `validate-on-migrate`, `baseline-on-migrate: false`)
- [x] Banco de dev recriado (`docker compose down -v && up -d`) — exatamente pelo drift que o `ddl-auto: update` causou (coluna `ativo` órfã, `status` faltando, `cpf varchar(14)`, `unique` fantasma em `nome_completo`, colunas FK duplicadas)
- [x] `contextLoads` com `validate`: `Successfully applied 1 migration ... now at version v1`
- [ ] (Futuro) Separar `application-dev.yaml`/`application-prod.yaml` — `show-sql` desligado em prod; ver item `7`
- [ ] (Futuro) Testcontainers para testes que sobem o banco sem depender do MySQL local

**Fluxo daqui em diante:** toda mudança de schema vira nova migration `V2__`, `V3__`...; nunca editar migration aplicada (checksum); usar `./mvnw flyway:info` / `flyway:repair` em dev.

---

## 5. Documentação da API + estratégia de versionamento — `AtletaController.java:20` `/v1/atletas` pendente

> `Versionamento: /v1/atletas` OK, mas documente estratégia (URL vs header).

**Status atual:** `AtletaController.java:20` `@RequestMapping("/v1/atletas")` usa versionamento via URL, correto (você fará na doc da API posteriormente). Sem OpenAPI (`springdoc` pendente) e sem doc da decisão URL vs header.

**Para implementar futuramente (junto da doc da API):**
- [ ] Adicionar `springdoc-openapi-starter-webmvc-ui` em `pom.xml`
- [ ] Anotar `AtletaController.java:20`:
  ```java
  @Tag(name="Atletas", description="API v1 - versionamento via URL /v1")
  // em cada método: @Operation(summary="...")
  ```
- [ ] Documentar decisão em `README.md` ou `docs/api-versioning.md`:
  ```
  # Versionamento
  Estratégia atual: URL (/v1/atletas). Alternativa descartada: header Accept: application/vnd.gestao.v1+json. Motivo: simplicidade + compatibilidade com browser.
  Evolução: /v2 mantém /v1 deprecated por 6 meses.
  ```
- [ ] Configurar `spring.jpa.open-in-view=false` (`application.yaml:12` pendente) junto do `springdoc` para evitar `LazyInitialization`
- [ ] Expor `swagger-ui.html` e validar `GET /v1/atletas?nome=&page=0` + `POST` com `Location` `AtletaController.java:50`
- [ ] Teste: `contextLoads` com `springdoc` + `ClockConfig`

---

## 6. Testes automatizados (`AtletaServiceTest`, `AtletaControllerTest`, `AtletaMapperTest`) — parcialmente feito

**Status atual:** já existem `AtletaServiceTest` (mock do `AtletaRepository` com Mockito), `NomeCompletoUtilsTest` (regra de normalização) e `AtletaRepositoryTest` (`@DataJpaTest`, cobre `@PrePersist`/`@PreUpdate`). Faltam `AtletaControllerTest` e `AtletaMapperTest`.

**Cenários mínimos a cobrir:**
- [x] `AtletaServiceTest` (mock `AtletaRepository` com Mockito):
  - `save` normaliza `cpf` e força `status=ATIVO`
  - `save` com CPF duplicado -> `CpfJaCadastradoException` (constraint `unique` + `AtletaService.java:44`)
  - `ativar`/`inativar` idempotentes via rich model
  - `update` bloqueia `INATIVO` -> `AtletaInativoException` (`Atleta.java:90`) e não copia `cpf`
  - `findAll` com `nome` null/blank chama `findAll(pageable)`; com texto, normaliza e chama `findByNomeCompletoNormalizadoStartingWith`
  - `findByIdOrThrowNotFound` -> `AtletaNotFoundException`
- [x] `NomeCompletoUtilsTest` (sem Spring): normalização (acento/caixa/espaço), símbolos -> vazio e `null` -> `NullPointerException`.
- [x] `AtletaRepositoryTest` (`@DataJpaTest` + H2): `@PrePersist` e `@PreUpdate` preenchem/renormalizam `nomeCompletoNormalizado`.
- [ ] `AtletaControllerTest` com `@WebMvcTest(AtletaController.class)` + `MockMvc`:
  - `POST /v1/atletas` válido -> `201` + header `Location` (`AtletaController.java:49`)
  - `POST` inválido -> `400` com `errors[].field` (`GlobalExceptionHandler.java:47`)
  - `PATCH /{id}/status` body `{ "status":"INATIVO" }` -> `200` (`AtletaController.java:55`)
  - `GET /v1/atletas/-1` -> `400` (`@Positive` + `ConstraintViolationException`)
  - `PUT /{id}` em atleta inativo -> `409`
- [ ] `AtletaMapperTest` (sem Spring):
  - `calcularIdade` com `Clock.fixed(Instant.parse("2026-09-13T00:00:00Z"), ZoneId.of("America/Recife"))` (`AtletaMapper.java:42`)
  - `toEntity(AtletaUpdateRequest)` não mapeia `cpf` (`AtletaMapper.java:29`)
  - `toGetResponse`/`toResumo` com `dataNascimento == null` -> `idade == null`
- [ ] Configurar `MockMvc` + `Clock` fixo (usar `@Import(ClockConfig.class)` com `@TestConfiguration` sobrescrevendo o bean — ver `config/ClockConfig.java`)

**Ferramentas/observações:**
- [ ] `src/test` já tem `spring-boot-starter-test` no `pom.xml` (JUnit 5 + Mockito + AssertJ)
- [ ] Testes de `@WebMvcTest` precisam mockar `AtletaService`/`AtletaMapper` (não sobem JPA nem MySQL)
- [x] `AtletaServiceTest` é unitário puro (não precisa Docker/MySQL); `@DataJpaTest` roda com H2 do profile `test` (também sem Docker) — confirmado em `AtletaRepositoryTest`.

---

## 7. Infra/Configuração — itens restantes do ponto `9`

> `Infra e Configuração`. (`open-in-view=false` já feito; ver abaixo.)

- [x] `spring.jpa.open-in-view=false` — ✅ Feito `application.yaml:11`. Evita `LazyInitializationException`/N+1 e o warning de startup. **Atenção futura:** ao wirar os domínios com `@ManyToOne(LAZY)` (`Equipe.java:27,31`, `InscricaoAtleta.java:26,30`, `InscricaoEquipe.java:28,32,36`, `Competicao.java:31`, `Categoria.java:31`), mapear DTO dentro de `@Transactional` do Service ou usar `@EntityGraph`/`JOIN FETCH` — não contar mais com OSIV.
- [ ] **Compatibilidade Java 25 + Spring Boot 4.1.0 + MapStruct 1.6.3** (`pom.xml:16,89-99`): combinação recente (2026). Verificar se `./mvnw clean compile` continua gerando `AtletaMapperImpl` corretamente e se não há warning de `annotationProcessor`; testar upgrade de MapStruct se surgir problema de Java 25.
- [ ] **Separar config dev vs prod** (`application.yaml:13-17`): hoje `show-sql:true` + `format_sql:true` sempre ligados. Criar `application-dev.yaml` (com `show-sql:true`/`format_sql:true` e `ddl-auto:update`) e `application-prod.yaml` (com `show-sql:false` + `ddl-auto:validate` + `logging.level.org.hibernate.SQL=DEBUG`). Relacionado ao item `ddl-auto → validate + Flyway` do checklist.
- [ ] **OpenAPI/springdoc** — já rastreado no item `5` (`doc API + versionamento`): adicionar `springdoc-openapi-starter-webmvc-ui`.

---

## Checklist Geral

- [ ] `hardDelete` — aguardando definição de `SecurityConfig` + `ADMIN`
- [ ] `findByCpfOrThrowNotFound` — aguardando decisão de exposição na API
- [x] Busca por nome — ✅ feito: coluna `nome_completo_normalizado` + índice BTREE + busca por prefixo (`V10`); `FULLTEXT` descartado (item `3`)
- [x] `ddl-auto` → `validate + Flyway` — ✅ feito: `V1__baseline.sql` + `spring-boot-starter-flyway`/`flyway-mysql` + `ddl-auto: validate` (item `4`)
- [ ] `doc API + versionamento` — `6` `AtletaController.java:20` `/v1` OK, doc pendente (OpenAPI + README)
- [ ] `Testes automatizados` — `8` `AtletaServiceTest`/`NomeCompletoUtilsTest`/`AtletaRepositoryTest` feitos; `AtletaControllerTest` + `AtletaMapperTest` pendentes
- [x] `open-in-view=false` — `9` feito em `application.yaml:11`
- [ ] `Infra ponto 9` — Java25/Boot4.1/MapStruct compat + separar `application-dev/prod.yaml` + OpenAPI (ver item `7`)

> Ao implementar os itens `1`/`2`, descomentar o método correspondente em `AtletaService.java` e expor o endpoint em `AtletaController.java`.
