# Estudos — Módulo Atleta / Gestão Esportiva

> Pasta `src/main/java/br/gov/quixada/esporte/iterations` — caderno de estudo do projeto.
> Origem: tópicos `10` (Plano de Estudo) e `12` (Referências) movidos de `src/main/resources/melhorias-atleta.md`.
> Uso: documentar tópicos ao longo do desenvolvimento para estudar/revisar depois.

---

## Plano de Estudo Sugerido

> Movido do ponto `10` de `melhorias-atleta.md`. Ordem: do menor risco ao maior impacto.

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

> Checklist de implementação desses pontos: ver seção `11. Checklist Prático` em `melhorias-atleta.md`.

---

## Referências

> Movido do ponto `12` de `melhorias-atleta.md`.

- Bean Validation 3.0 (Jakarta) — `@CPF`, `@Past`, `@Valid`
- Spring Data JPA — `JpaRepository`, `Pageable`, `ContainingIgnoreCase`
- MapStruct 1.6.3 docs — `componentModel="spring"`, `@Context`, `expression`
- Spring Boot 3.x/4.x — `@RestControllerAdvice`, `ProblemDetail` (RFC 7807)
- Hibernate — `@CreationTimestamp`, `@PrePersist`, dirty checking
- MySQL — `LIKE %x%` vs `FULLTEXT`, collation `utf8mb4_unicode_ci`
- REST — `201 Created` + `Location`, idempotência `PUT` vs `PATCH`

> Dica: cada item da análise pode virar um commit pequeno. Abra um branch `estudos/atleta-melhorias` e faça PRs atômicos — facilita revisão e aprendizado.

---

## Tópicos de Estudo Acumulados

> Adicionar aqui, ao longo do desenvolvimento, conceitos/práticas que surgirem e mereçam revisão futura.
> Formato sugerido: `### <tópico>`, contexto (onde apareceu no código), o que estudar, links/artigos.

<!-- Modelo:
### <Tópico>
- **Onde apareceu:** `Arquivo.java:linha`
- **O que estudar:** ...
- **Referências:** ...
-->

### Rich Domain Model (anemic vs rich)
- **Onde apareceu:** `Atleta.java` (ponto `4.1`), `AtletaService.java`.
- **O que estudar:** diferença entre entidade anêmica (só getters/setters) e rica (métodos de domínio `ativar()`, `inativar()`, `atualizarDados()`); encapsulamento de invariantes; `@Setter(AccessLevel.PRIVATE/PACKAGE)`; JPA `field access` via reflection.
- **Decisões do projeto:** ver `decisoes.md` (handler) e `melhorias-atleta.md` ponto `4.1`.

### Open Session In View (OSIV)
- **Onde apareceu:** `application.yaml:11` (`open-in-view: false`, ponto `9`).
- **O que estudar:** o que é OSIV, por que o default é `true`, `LazyInitializationException`, N+1, `@EntityGraph`, `JOIN FETCH`, mapear DTO dentro do `@Transactional`.
- **Atenção:** `@ManyToOne(LAZY)` em `Equipe`, `InscricaoAtleta`, `InscricaoEquipe`, `Competicao`, `Categoria` (ainda stubs).

### Estratégias de versionamento de API
- **Onde apareceu:** `AtletaController.java` `/v1/atletas` (ponto `6`).
- **O que estudar:** versionamento por URL vs header (`Accept: application/vnd...+json`) vs media type; deprecação; compatibilidade.

### `DataIntegrityViolationException` + corrida de CPF (TOCTOU)
- **Onde apareceu:** `AtletaService.java:44-53` (`save`).
- **Contexto:** no `3.4` removeu-se o `existsByCpf()` (que era uma checagem TOCTOU: "check-then-act" sujeita a corrida). Passou-se a confiar na constraint `unique` do banco (`Atleta.java:46`).
- **Decisão:** no `catch`, checar o erro **específico** de duplicidade do MySQL (`getMostSpecificCause()` + `SQLIntegrityConstraintViolationException` + `errorCode == 1062`) e só então traduzir para `CpfJaCadastradoException`; caso contrário `rethrow` → tratado pelo `GlobalExceptionHandler` como `409 "Conflito de dados"`.
- **O que estudar:** por que não capturar `DataIntegrityViolationException` de forma genérica (mascaramento de NOT NULL / data too long / FK); hierarquia de causas (`getMostSpecificCause()`); códigos MySQL (`1062` duplicate, `1048` not-null, `1452` FK); alternativa frágil de inspecionar nome da constraint por causa do `ddl-auto: update` (`UK_<hash>`).

### Fuso horário (`Clock`) e cálculo de idade
- **Onde apareceu:** `ClockConfig.java:13`.
- **Decisão:** `Clock.system(ZoneId.of("America/Fortaleza"))` (Quixadá-CE; Ceará usa `America/Fortaleza`, UTC-3 sem horário de verão), em vez de `systemDefaultZone()` (que depende da JVM e pode divergir do `serverTimezone=America/Recife` do datasource em `application.yaml:6`).
- **O que estudar:** `java.time.Clock`, `ZoneId`, `LocalDate.now(clock)`, como injetar `Clock` fixo em testes (`Clock.fixed(...)`), por que `LocalDate.now()` sem `Clock` é não-determinístico.

