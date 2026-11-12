# Pendências — Gestão Esportiva API

> Arquivo consolidado em 2026-11-09; atualizado em 2026-11-12 para marcar itens concluídos. Reúne **tudo que ainda não foi implementado ou está pendente** no projeto, extraído de `decisoes.md`, `estudos.md`, `fixes.md`, `futuras-iterações.md` e `securityfixes.md` (removidos após a consolidação).
>
> Itens concluídos permanecem marcados com `[x]` para rastreabilidade; os pendentes ficam com `[ ]`.
>
> Prioridades: **P0** crítico/explorável · **P1** importante · **P2** melhoria/boa prática.

---

## Checklist

### Segurança / Autenticação
- [x] **A5** — Rate limiting no login (bucket4j, sem lockout) (P0)
- [ ] **C6** — Refresh token / logout / revogação (P2)
- [ ] **C7** — (Opcional) roles como claim do JWT (P2)
- [x] **C8** — Configurar CORS (P2)

### Usuários / Governança
- [x] Decisão: exatamente **1 SUPERADMIN**, sem criação/promoção pela API (bloqueios já existentes em `UserService`)
- [x] Recuperação de senha do SUPERADMIN via CLI (P1)
- [ ] Auditoria: filtros por ator/alvo/período, retenção/expurgo e exportação (P2)

### Atleta
- [x] `hardDelete` + endpoint `DELETE /v1/atletas/{id}` (P1)
- [ ] `AtletaControllerTest` (P1)
- [x] `AtletaMapperTest` (P1)
- [x] Configurar `MockMvc` + `Clock` fixo nos testes de controller (P1)

### Testes dos novos fluxos
- [ ] Testes de controller/mapper para competição, inscrição, partida e classificação (P1)

### Core esportivo
- [ ] Geração automática de rodadas (turno único / returno) (P1)
- [ ] Mínimo / máximo de elenco — decisão com equipe técnica (P1)

### Infra / Documentação
- [ ] OpenAPI/springdoc + `@Tag`/`@Operation` (P2)
- [x] README + documentação da estratégia de versionamento (P2)
- [x] `application-prod.yaml` + separar config dev/prod (P2)
- [x] Verificar compatibilidade Java 25 + Spring Boot 4.1.0 + MapStruct 1.6.3 (P2)
- [x] CI (GitHub Actions) rodando `./mvnw verify` (P2)
- [ ] Testcontainers (opcional) (P2)

---

## Detalhamento

### A5 — Rate limiting no login (P0) — Concluído
- **Onde:** `security/auth/AuthController#login`, `security/auth/LoginRateLimiter`, `security/config/LoginRateLimitProperties`, `security/config/ClockTimeMeter`, `security/config/RateLimitConfig`, `security/auth/exception/MuitasTentativasException`, `common/error/GlobalExceptionHandler`.
- **Situação anterior:** tentativas infinitas; `UserDetailsAdapter.isAccountNonLocked()` retornava sempre `true`; não havia `bucket4j`/`Resilience4j` no `pom.xml`.
- **Decisão:** **rate limiting, não lockout de conta**, com **bucket4j core** (`bucket4j_jdk17-core:8.22.0`, versão explícita) + **Caffeine** (versão gerenciada pelo Boot) para limitar memória. Sem migration e sem estado persistente. `Resilience4j` fica reservado para chamadas externas futuras (retry/circuit breaker).
- **Modelo:** dois buckets em memória por tentativa — por **IP** (`max-por-ip`) e por **(IP, e-mail normalizado)** (`max-por-ip-email`). Nunca por e-mail isolado: permitiria bloquear a conta de terceiros (DoS). O consumo acontece **antes** do BCrypt; login com sucesso devolve a tentativa (`addTokens`). Excedeu → `429` no formato `ApiError` + header `Retry-After` (segundos, de `getNanosToWaitForRefill`), com mensagem genérica.
- **Config:** `api.security.login-rate-limit.{max-por-ip:100, max-por-ip-email:5, janela:15m, maximo-chaves:100000}`. Em prod, tunável por `API_LOGIN_RATE_LIMIT_*` (com default).
- **Semântica:** `refillGreedy` ⇒ 5 tentativas/15 min = 1 token a cada 3 min. É throttle suave, não bloqueio duro de 15 min.
- **Limitações:** estado em memória e por instância — N réplicas ⇒ limite efetivo N×; restart zera contadores; evicção do Caffeine (`maximo-chaves`) recria buckets cheios (fail-open); evolução é trocar o `Cache<String,Bucket>` por `ProxyManager` (Redis/MySQL). IP vem de `request.getRemoteAddr()`; atrás de proxy confiável, configurar `server.forward-headers-strategy` (senão todo o tráfego aparece como o IP do proxy).
- **Testes:** `LoginRateLimiterTest` (8), `AuthControllerLoginRateLimitTest` (4) e `LoginRateLimitPropertiesTest` (3), sem `Thread.sleep` (`ClockTimeMeter` + `MutableClock`; IP via `request.setRemoteAddr`).

### C6 — Refresh token / logout / revogação (P2)
- **Onde:** fluxo de auth como um todo (`security/jwt/JwtService`, `security/auth/AuthController`).
- **Situação:** o token dura `2h` (`api.security.token.expiration`) e não há refresh nem logout no servidor.
- **Por quê:** ao expirar, o usuário precisa logar de novo; não há como invalidar um token antes do vencimento (ex.: troca de senha, logout). JWT puro não resolve.
- **Como implementar:** definir a estratégia de sessão de longo prazo — refresh token + blacklist/allowlist. O pacote `security/jwt` já foi separado justamente para preparar isso.
- **Como testar:** revogar token e verificar que passa a responder `401`; refresh emite novo access token.

### C7 — (Opcional) roles como claim do JWT (P2)
- **Onde:** `security/jwt/JwtService` + `security/config/JwtAuthenticationFilter`.
- **Situação:** o filtro faz **um SELECT no banco por request** para carregar o usuário e suas authorities.
- **Trade-off:** colocar as roles no token evita a consulta, mas perde a revogação imediata (token antigo mantém a role velha até expirar). Desempenho × revogação — decidir.

### C8 — Configurar CORS (P2) — Concluído
- **Onde:** `security/config/CorsProperties`, `SecurityConfig`, `application.yaml` / `application-prod.yaml`.
- **Como ficou:** `api.cors.allowed-origins` (lista, via `API_CORS_ALLOWED_ORIGINS`) → bean `CorsConfigurationSource` registrado em `/**` e aplicado por `.cors(...)` no `SecurityFilterChain`. Métodos `GET/POST/PUT/PATCH/DELETE/OPTIONS`; headers `Authorization` e `Content-Type`; `Location` exposto; `maxAge` 1h.
- **Comportamento:** preflight de origem não listada → `403`; origem listada → `200` com `Access-Control-Allow-Origin`.
- **Testes:** `CorsConfigTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`) cobre origem permitida e origem invasora.
- **Atenção em prod:** o boot falha se `API_CORS_ALLOWED_ORIGINS` não for informada.

### Recuperação de senha do SUPERADMIN via CLI (P1) — Concluído
- **Status:** implementado no commit `f97544d`. Runbook em `docs/operacao/recuperacao-superadmin.md`.
- **Decisão:** o sistema mantém **exatamente 1 SUPERADMIN**, responsável por gerir os `ADMIN`. A API continua bloqueando criar/promover `SUPERADMIN` (`UserService`) e não há endpoint de recuperação exposto na rede.
- **Onde:** `security/users/SuperadminPasswordResetService`, `security/users/cli/SuperadminPasswordResetRunner`, profile `superadmin-reset` (`application-superadmin-reset.yaml`).
- **Por quê:** sem e-mail/MFA, o risco real é perder a senha do único SUPERADMIN. A recuperação exige acesso ao shell do servidor — mesmo nível de privilégio que já permitiria alterar o banco — então não adiciona superfície de ataque remota.
- **Como funciona:** `java -jar app.jar --spring.profiles.active=dev,superadmin-reset` (ou `prod,superadmin-reset`); o runner lê o e-mail (`--reset.email`/`SUPERADMIN_EMAIL`, default `admin@quixada.gov.br`) e a senha (console interativo ou `SUPERADMIN_NEW_PASSWORD`), re-hasheia com BCrypt, reativa a conta e audita `PASSWORD_RESET`, então encerra.
- **Limitação:** JWT é stateless — tokens emitidos antes do reset seguem válidos até expirar (2h); revogação imediata depende do item C6.
- **Como testar:** `SuperadminPasswordResetServiceTest` cobre sucesso, e-mail inexistente, usuário não-SUPERADMIN e senha fora do intervalo.

### Auditoria de usuários: filtros, retenção/expurgo e exportação (P2)
- **Onde:** `security/users/UserController#findAudit` (`GET /v1/users/audit`), `security/users/UserAuditService#findAll`, `UserAuditRepository`, tabela `db_user_audit` (`V18`).
- **Situação:** existe apenas `findAll(Pageable)` — sem filtros e sem exportação; não há política de retenção/expurgo.
- **Como implementar:** filtros por ator/alvo/ação e intervalo de datas (Specifications), endpoint de exportação (CSV/JSON) e rotina/job de retenção.
- **Como testar:** `GET /v1/users/audit?actorEmail=...&from=...&to=...`; expurgo remove registros fora da janela.

### `hardDelete` de atleta + endpoint (P1) — Concluído
- **Status:** implementado. `AtletaService#hardDelete` + `DELETE /v1/atletas/{id}`.
- **Decisão da equipe:** exclusão física **somente no atleta**. Os demais domínios não são apagados: usam `status` (`INATIVO`/`CANCELADA`) + campo `observacao` livre para registrar qualquer informação (motivo de cancelamento, prorrogação de datas, avisos).
- **Regra:** exige atleta `INATIVO` (senão `409`) e nenhuma inscrição em `OCUPAM_VAGA` (`PENDENTE`/`APROVADA`/`INATIVA`). As inscrições em `LIBERAM_VAGA` (`CANCELADA`/`REJEITADA`) são apagadas junto. Retorna `204`; `404` se não existir.
- **Impacto verificado:** `db_atletas` só é referenciado por `db_inscricao_atleta`; classificação não usa inscrição de atleta e não há auditoria de atleta. Efeito colateral assumido: o `unique` de CPF é liberado.
- **Testes:** `AtletaServiceTest` (nested `hardDelete`) e `AtletaHardDeleteRepositoryTest` (FK bloqueia delete com inscrição; `deleteByAtletaIdAndStatusIn` libera o caminho).

### Campo `observacao` + `data_cancelamento` (V21) — Concluído
- **Decisão da equipe:** em vez de excluir fisicamente competição/inscrição/partida, registrar o motivo/contexto em um campo livre.
- **`observacao`:** texto livre (`varchar(500)`, opcional) em `Competicao`, `InscricaoEquipe`, `InscricaoAtleta` e `Partida`. **Sem vínculo com status** — pode ser criada no `POST`, editada ou limpa a qualquer momento via `PATCH /v1/{recurso}/{id}/observacao`, inclusive em registros `CANCELADA`/`FINALIZADA`/`INATIVO`. Ausente/em branco ⇒ `null`. Não é propagada em cascata.
- **`dataCancelamento`:** informativa, gravada apenas na transição para `CANCELADA` (inclui o bulk update de partidas).
- **Fora das listagens:** os `*ResumoResponse` não expõem `observacao`.

### `AtletaControllerTest` (P1)
- **Onde:** `src/test/java/.../atleta/` (não existe).
- **Cenários mínimos:** `POST /v1/atletas` válido → `201` + header `Location`; `POST` inválido → `400` com `errors[].field`; `PATCH /{id}/status` → `200`; `GET /v1/atletas/-1` → `400`; `PUT /{id}` em atleta inativo → `409`.
- **Atenção:** controller versionado exige `ApiVersionStrategy` no MockMvc standalone (ver `AuthControllerTest`), senão lança `IllegalStateException`.

### `AtletaMapperTest` (P1) — Concluído
- **Onde:** `src/test/java/.../atleta/AtletaMapperTest.java`.
- **Como ficou:** instancia `AtletaMapperImpl` diretamente e injeta `EnderecoMapperImpl` via `ReflectionTestUtils` (o impl gerado usa field injection). 9 casos: `calcularIdade` com `Clock.fixed` (incluindo véspera de aniversário e data nula); `toEntity(AtletaUpdateRequest)` não mapeia `cpf`, `id`, `dataCadastro` nem `status`; `toGetResponse`/`toResumo` calculam idade e devolvem `null` quando `dataNascimento` é nulo.

### Configurar `MockMvc` + `Clock` fixo nos testes de controller (P1) — Concluído
- **Onde:** `src/test/java/br/gov/quixada/esporte/support/ControllerTestSupport.java`, `config/ClockTestConfig.java`.
- **Como ficou:** `ControllerTestSupport` centraliza o MockMvc standalone (com `GlobalExceptionHandler` + `ApiVersionStrategy`) e expõe `FIXED_CLOCK` (`2026-06-15T12:00:00Z`, `America/Fortaleza`). `ClockTestConfig` é um `@TestConfiguration` com `@Primary` para sobrescrever o bean `Clock` em testes que sobem o contexto (`@Import(ClockTestConfig.class)`).
- **Adoção:** `AuthControllerTest` e `CompeticaoControllerTest` passaram a usar `ControllerTestSupport.mockMvc(...)`, removendo a duplicação da estratégia de versionamento. `AtletaControllerClockTest` valida a idade calculada com o clock fixo em `GET` e `POST`; `ClockTestConfigTest` garante que o bean fixo tem prioridade.

### Testes de controller/mapper dos novos fluxos (P1)
- **Onde:** `src/test/java/.../` — hoje existem `AuthControllerTest`, `CompeticaoControllerTest` (observação) e `AtletaMapperTest`.
- **Escopo faltante:** competição, inscrição (`InscricaoAtleta`/`InscricaoEquipe`), partida e classificação — controllers e mappers.

### Geração automática de rodadas (turno único / returno) (P1)
- **Onde:** `partida/` (`Partida.rodada`, `PartidaCreateRequest.rodada`).
- **Situação:** `rodada` é um campo informado manualmente; não há geração automática de tabela de jogos.
- **Como implementar:** gerar confrontos a partir das inscrições `APROVADA` da competição/categoria (round-robin, turno único/returno), respeitando a regra de "uma equipe por rodada/horário".
- **Como testar:** N equipes → quantidade/estrutura esperada de partidas; returno inverte mandante/visitante.

### Mínimo / máximo de elenco (P1)
- **Onde:** regras de `equipe`/`inscricao`.
- **Situação:** não há validação de tamanho mínimo/máximo de elenco.
- **Bloqueio:** depende de decisão com a equipe técnica sobre os limites por modalidade/categoria.
- **Como implementar:** após a definição, validar no `InscricaoEquipeService` e cobrir em testes.

### OpenAPI/springdoc + anotações (P2)
- **Onde:** `pom.xml` (sem `springdoc-openapi-starter-webmvc-ui`), controllers.
- **Como implementar:** adicionar a dependência, anotar controllers com `@Tag`/`@Operation`, expor `swagger-ui.html` e validar `GET /v1/atletas?nome=&page=0` e `POST` com `Location`.
- **Atenção:** `spring.jpa.open-in-view=false` já está ligado — mapear DTOs dentro do `@Transactional` ou usar `@EntityGraph`/`JOIN FETCH`.

### README + documentação da estratégia de versionamento (P2) — Concluído
- **Status:** `README.md` criado, com setup, autenticação/papéis, tabela de endpoints, testes e a decisão de versionamento (URL `/v1`; alternativa por header descartada; política de deprecação).
- **Observação:** o versionamento nativo já estava implementado (`/v{version}/...`, `version = "1.0"`); faltava apenas a documentação, agora publicada.

### `application-prod.yaml` + separar config dev/prod (P2) — Concluído
- **Como ficou:** `application.yaml` deixou de ligar `show-sql`/`format_sql` (agora só em `dev`); `application-dev.yaml` ganhou `show-sql: true`, `format_sql` e `spring.docker.compose.enabled: true`; `application-prod.yaml` traz `show-sql: false`, `format_sql: false`, `ddl-auto: validate`, Flyway on, Docker Compose off, `server.port` configurável e níveis de log.
- **Variáveis exigidas em prod:** `DB_URL`, `DB_USER`, `DB_PASSWORD`, `API_CORS_ALLOWED_ORIGINS`, `API_SECURITY_TOKEN_SECRET` (boot falha sem elas).

### Verificar compatibilidade Java 25 + Boot 4.1.0 + MapStruct 1.6.3 (P2) — Concluído
- **Resultado:** `./mvnw clean compile` com JDK 25.0.4.1 gera todos os `*MapperImpl` normalmente. A verificação **achou e corrigiu** dois warnings de `Unmapped target property: "nomeCompletoNormalizado"` em `AtletaMapper` (adicionado `@Mapping(ignore = true)` nos dois `toEntity`). Hoje o build não emite nenhum `[WARNING]` do compilador.
- **Pendência residual:** o JDK 25 imprime o aviso `sun.misc.Unsafe::objectFieldOffset ... terminally deprecated` disparado pelo `lombok.permit.Permit` (Lombok 1.18.46, já a versão mais recente). É inofensivo e não tem correção do lado do projeto; acompanhar quando o Lombok lançar uma implementação sem `Unsafe`.

### CI (GitHub Actions) (P2) — Concluído
- **Onde:** `.github/workflows/ci.yml`.
- **Como ficou:** workflow disparado em `push`/`pull_request` para `dev` e `main`, com JDK 25 (Temurin), cache Maven e `./mvnw --batch-mode --no-transfer-progress verify` (perfil `test`/H2, sem MySQL). `permissions: contents: read` e `concurrency` por ref.

### Testcontainers (opcional) (P2)
- **Onde:** testes de integração.
- **Como implementar:** subir MySQL real em container para testes que dependam do dialeto/constraints do MySQL (hoje os testes usam H2 — o branch de `errorCode == 1062` do CPF não é exercitado).

---

## Bloqueios / decisões necessárias antes de implementar
- **Mínimo/máximo de elenco:** depende da equipe técnica (limites por modalidade/categoria).
- **Criação de `SUPERADMIN` pela API:** decidido — exatamente 1 SUPERADMIN, sem endpoint de criação/promoção; recuperação de senha via CLI.
- **C6/C7:** definir trade-off entre revogação imediata e desempenho (SELECT por request × roles no token).
