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

**Referência:** `melhorias-atleta.md:4.4` `AtletaService.java:33` código morto — ou expor `GET /cpf` ou remover.

---

## 2. `findByCpfOrThrowNotFound(String cpf)` — `AtletaService.java:32-36` comentado

```java
//    @Transactional(readOnly = true)
//    public Atleta findByCpfOrThrowNotFound(String cpf) {
//        return repository.findByCpf(CpfUtils.normalize(cpf))
//                .orElseThrow(() -> new AtletaNotFoundException("Atleta não encontrado"));
//    }
```

**Por que está comentado:** Método já correto (usa `CpfUtils.java:8` `normalize` + `AtletaRepository.java:18` `findByCpf`), mas `AtletaController.java:19` nunca expõe `GET /v1/atletas/cpf/{cpf}` — fica código morto como apontado em `melhorias-atleta.md:207`.

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

## 3. Índice MySQL `FULLTEXT` para `findByNomeCompletoContainingIgnoreCase` — `AtletaService.java:22` / `AtletaRepository.java:18` pendente

> Ponto `5` `melhorias-atleta.md:222` `findByNomeCompletoContaining` sem índice → `FULL TABLE SCAN` com `LIKE %x%`.

**Status atual:** `AtletaService.java:22` `Page<Atleta> findAll(nome, pageable)` + `AtletaRepository.java:18` `findByNomeCompletoContainingIgnoreCase(name, pageable)` já implementados com `Pageable` + `IgnoreCase` (ver `2.4`), mas `Atleta.java:33` `@Table(name="db_atletas")` sem `indexes`. Sem índice, `LIKE %joao%` faz scan em 10k+ registros.

**Para implementar no futuro:**
- [ ] Adicionar em `Atleta.java:33`:
  ```java
  @Table(name="db_atletas", indexes=@Index(name="idx_atleta_nome", columnList="nomeCompleto"))
  // ou para busca textual: FULLTEXT se migrar para MyISAM/InnoDB 5.6+
  ```
- [ ] Ou criar migration `db/migration/V2__add_fulltext_nome_atleta.sql`:
  ```sql
  CREATE FULLTEXT INDEX idx_atleta_nome_fulltext ON db_atletas(nomeCompleto);
  -- query passa a usar MATCH(nomeCompleto) AGAINST(:nome IN BOOLEAN MODE) via @Query
  ```
- [ ] Avaliar alternativa `@Query` com `MATCH` vs manter `ContainingIgnoreCase` + índice `BTREE` simples (suficiente se `AtletaService.java:22` já pagina)
- [ ] Validar com `EXPLAIN SELECT ... WHERE nomeCompleto LIKE '%joao%'` antes/depois
- [ ] Teste de performance: `AtletaRepositoryTest#findByNomeCompletoContainingIgnoreCase_comIndice_deveUsarIndex`

**Referência:** `melhorias-atleta.md:5` `Performance e Persistência`.

---

## 4. `ddl-auto:update` → `validate` + Flyway/Liquibase — `application.yaml:12` pendente

> Ponto `5` `melhorias-atleta.md:224` `ddl-auto:update` perigoso em prod (pode dropar coluna).

**Status atual:** `src/main/resources/application.yaml:12` ainda `ddl-auto: update` + `show-sql:true` + `format_sql:true`. Sem `validate` + Flyway (`src/main/resources/db/migration/` vazio, `HELP.md` menciona Flyway mas `pom.xml:16` sem dependência).

**Para implementar no futuro:**
- [ ] Adicionar em `pom.xml:16`:
  ```xml
  <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-mysql</artifactId></dependency>
  ```
- [ ] Criar `V1__create_db_atletas.sql` espelhando `Atleta.java:33` (`db_atletas`, `cpf unique`, `nomeCompleto 150`, `status`, etc) + `V2` do índice acima
- [ ] Trocar `application.yaml:12`:
  ```yaml
  spring.jpa.hibernate.ddl-auto: validate
  spring.flyway.enabled: true
  spring.flyway.locations: classpath:db/migration
  ```
- [ ] Manter `show-sql:true` só em `application-dev.yaml`, desligar em `application-prod.yaml` (`logging.level.org.hibernate.SQL: DEBUG`)
- [ ] Verificar compatibilidade `ddl-auto: validate` com `Atleta.java:28` `@Builder`/`@PrePersist` (não deve quebrar)
- [ ] Teste: `./mvnw flyway:migrate` + `contextLoads` com `validate`

**Referência:** `melhorias-atleta.md:9` `Infra e Configuração` + `HELP.md` Flyway boilerplate.

---

## 5. Documentação da API + estratégia de versionamento — `AtletaController.java:20` `/v1/atletas` pendente

> Ponto `6` `melhorias-atleta.md:232` `Versionamento: /v1/atletas OK, mas documente estratégia (URL vs header)`.

**Status atual:** `AtletaController.java:20` `@RequestMapping("/v1/atletas")` usa versionamento via URL, correto (você fará na doc da API posteriormente). Sem OpenAPI (`src/main/resources/melhorias-atleta.md:9` `open-in-view`/`springdoc` pendente) e sem doc da decisão URL vs header.

**Para implementar futuramente (junto da doc da API):**
- [ ] Adicionar `springdoc-openapi-starter-webmvc-ui` em `pom.xml:16` (ver `melhorias-atleta.md:9`)
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

**Referência:** `melhorias-atleta.md:6` `API REST e Controller` + `melhorias-atleta.md:9` `Infra`.

---

## 6. Testes automatizados (`AtletaServiceTest`, `AtletaControllerTest`, `AtletaMapperTest`) — `src/test/**/*Atleta*` vazio

> Ponto `8` `melhorias-atleta.md:266` `Sem testes: src/test/**/*Atleta* vazio`.

**Status atual:** só existe `src/test/java/br/gov/quixada/esporte/GestaoEsportivaApiApplicationTests.java` (`contextLoads()`). Nenhum teste de `Atleta`. Módulo já refatorado (rich model `4.1`, `Clock` `4.2`, paginação `2.4`, handlers `7`), então é o próximo passo natural para travar as regras.

**Cenários mínimos a cobrir:**
- [ ] `AtletaServiceTest` (mock `AtletaRepository` com Mockito):
  - `save` normaliza `cpf` e força `status=ATIVO` (`AtletaService.java:38`)
  - `save` com CPF duplicado -> `CpfJaCadastradoException` (constraint `unique` `Atleta.java:50` + `AtletaService.java:44`)
  - `ativar`/`inativar` idempotentes via rich model (`AtletaService.java:56-68`)
  - `update` bloqueia `INATIVO` -> `AtletaInativoException` (`Atleta.java:90`) e não copia `cpf`
  - `findAll` com `nome` null/blank chama `findAll(pageable)`, com texto chama `findByNomeCompletoContainingIgnoreCase`
  - `findByIdOrThrowNotFound` -> `AtletaNotFoundException`
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
- [ ] `AtletaServiceTest` é unitário puro (não precisa Docker/MySQL); teste de integração só se usar `@DataJpaTest` (aí precisa do banco `docker compose up -d`)

**Referência:** `melhorias-atleta.md:8` `Testabilidade e Qualidade` + `melhorias-atleta.md:10` Semana 4.

---

## 7. Infra/Configuração — itens restantes do ponto `9`

> Ponto `9` `melhorias-atleta.md:276` `Infra e Configuração`. (`open-in-view=false` já feito agora; ver abaixo.)

- [x] `spring.jpa.open-in-view=false` — ✅ Feito `application.yaml:11`. Evita `LazyInitializationException`/N+1 e o warning de startup. **Atenção futura:** ao wirar os domínios com `@ManyToOne(LAZY)` (`Equipe.java:27,31`, `InscricaoAtleta.java:26,30`, `InscricaoEquipe.java:28,32,36`, `Competicao.java:31`, `Categoria.java:31`), mapear DTO dentro de `@Transactional` do Service ou usar `@EntityGraph`/`JOIN FETCH` — não contar mais com OSIV.
- [ ] **Compatibilidade Java 25 + Spring Boot 4.1.0 + MapStruct 1.6.3** (`pom.xml:16,89-99`): combinação recente (2026). Verificar se `./mvnw clean compile` continua gerando `AtletaMapperImpl` corretamente e se não há warning de `annotationProcessor`; testar upgrade de MapStruct se surgir problema de Java 25.
- [ ] **Separar config dev vs prod** (`application.yaml:13-17`): hoje `show-sql:true` + `format_sql:true` sempre ligados. Criar `application-dev.yaml` (com `show-sql:true`/`format_sql:true` e `ddl-auto:update`) e `application-prod.yaml` (com `show-sql:false` + `ddl-auto:validate` + `logging.level.org.hibernate.SQL=DEBUG`). Relacionado ao item `ddl-auto → validate + Flyway` do checklist.
- [ ] **OpenAPI/springdoc** — já rastreado no item `5` (`doc API + versionamento`): adicionar `springdoc-openapi-starter-webmvc-ui`.

**Referência:** `melhorias-atleta.md:9` `Infra e Configuração`.

---

## Checklist Geral

- [ ] `hardDelete` — aguardando definição de `SecurityConfig` + `ADMIN`
- [ ] `findByCpfOrThrowNotFound` — aguardando decisão de exposição na API
- [ ] `FULLTEXT` / índice `nomeCompleto` — `5` paginação pronta, índice pendente
- [ ] `ddl-auto` → `validate + Flyway` — aguardando config `application.yaml:12` + `db/migration`
- [ ] `doc API + versionamento` — `6` `AtletaController.java:20` `/v1` OK, doc pendente (OpenAPI + README)
- [ ] `Testes automatizados` — `8` `AtletaServiceTest` + `AtletaControllerTest` + `AtletaMapperTest` pendentes (`src/test/**/*Atleta*` vazio)
- [x] `open-in-view=false` — `9` feito em `application.yaml:11`
- [ ] `Infra ponto 9` — Java25/Boot4.1/MapStruct compat + separar `application-dev/prod.yaml` + OpenAPI (ver item `7`)

> Quando implementar, descomentar em `AtletaService.java`, expor em `AtletaController.java:19` e atualizar `melhorias-atleta.md:207` `4.4` e `5`.
