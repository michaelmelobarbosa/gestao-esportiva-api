# AGENTS.md

## Stack
- Java 25 + Spring Boot 4.1.0 + Maven wrapper (`./mvnw`, Maven 3.9.16). `pom.xml:16` pins `java.version=25` — ensure JDK 25 is active (`java -version`).
- Spring Data JPA + MySQL 8.4 (`mysql-connector-j` runtime) in `dev`; H2 in-memory (`com.h2database:h2`, test scope) in `test`.
- **Flyway is active** (`spring-boot-starter-flyway` + `flyway-mysql`, `pom.xml:44-51`). Migrations live in `src/main/resources/db/migration/` (`V1__baseline.sql` .. `V20__create_partida.sql`). `ddl-auto: validate` in dev — schema comes from migrations, not Hibernate. Never edit an applied migration (checksum breaks); add `V21__...`.
- MapStruct 1.6.3 + Lombok. Both must stay in `pom.xml:103-113` `maven-compiler-plugin.annotationProcessorPaths` — removing either breaks generated mappers.

## Environments / Profiles
Three Spring profiles; default is `dev` (`application.yaml:8-9`, overridable via `SPRING_PROFILES_ACTIVE`).
- **dev** (`application-dev.yaml`): MySQL `localhost:3306/gestao_esportiva`, credentials from `.env`, Flyway on, `ddl-auto: validate`, `show-sql: true` (SQL logging lives here, not in the default file).
- **test** (`application-test.yaml`): H2 in-memory `jdbc:h2:mem:gestao_esportiva_test;MODE=MySQL`, Flyway off, `ddl-auto: create-drop`, Spring Docker Compose off, `show-sql: false`. Surefire forces it — `pom.xml:125-127` sets `spring.profiles.active=test`.
- **prod** (`application-prod.yaml`): `show-sql`/`format_sql` off, Flyway on, `ddl-auto: validate`, Docker Compose off. Requires `DB_URL`, `DB_USER`, `DB_PASSWORD`, `API_CORS_ALLOWED_ORIGINS` and `API_SECURITY_TOKEN_SECRET` (boot fails without them).
- H2 is test-scoped only. Tests no longer need Docker/MySQL: service tests are pure Mockito; `AtletaRepositoryTest` uses `@DataJpaTest` and `contextLoads` boots the full context (both on H2).
- **CORS**: `api.cors.allowed-origins` (`CorsProperties`) → `CorsConfigurationSource` bean in `SecurityConfig`. Unknown origins get `403` on preflight; covered by `CorsConfigTest`.

## Commands
```bash
cp .env.example .env  # fill MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE, MYSQL_ROOT_PASSWORD, API_SECURITY_TOKEN_SECRET
docker compose up -d  # starts gestao-esportiva-mysql on 3306 (healthcheck 30s start_period)
./mvnw spring-boot:run          # profile dev (default); Docker daemon required — spring-boot-docker-compose auto-starts compose.yaml
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run   # explicit dev
./mvnw verify                   # full build; tests use profile test + H2, no DB needed
./mvnw test                     # uses profile test + H2, no MySQL needed
./mvnw -Dtest=GestaoEsportivaApiApplicationTests#contextLoads test  # single test
./mvnw package -DskipTests
```

## Database / Env
- `.env` is gitignored; committed `.env` (dev) has `esporte/esporte/gestao_esportiva/root`. Loaded via `spring.config.import: optional:file:.env[.properties]` (`application.yaml:3`) and read at `application-dev.yaml:4-5`.
- URL dev: `jdbc:mysql://localhost:3306/gestao_esportiva?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=America/Recife` (`application-dev.yaml:3`).
- `spring-boot-docker-compose` (`pom.xml`, runtime) manages `compose.yaml` lifecycle on `spring-boot:run`; disabled under test (`application-test.yaml`).
- Migrations are the source of truth. `application.yaml` keeps `show-sql`/`format_sql` on by default; test turns SQL logging off.
- **Config do JWT**: `TokenProperties` (`@ConfigurationProperties(prefix = "api.security.token")`, `@Validated`) agrupa `secret` (`min 32`), `issuer` e `expiration` (`Duration`, ex. `2h`). Sem `API_SECURITY_TOKEN_SECRET`, o boot falha. Dev usa o valor do `.env`; testes usam o valor fixo de `application-test.yaml`.
- **`Clock` no JWT**: `JwtService` injeta o bean `Clock` (`ClockConfig`) e calcula `exp` com `clock.instant().plus(properties.expiration())` — nunca use `LocalDateTime.now()` com offset fixo.

## Architecture
- Entrypoint: `src/main/java/br/gov/quixada/esporte/GestaoEsportivaApiApplication.java`.
- All domains are now fully wired (entity → repository → service → controller → MapStruct mapper → DTO records): `atleta`, `categoria`, `clube`, `competicao`, `equipe`, `modalidade`, `inscricao` (`InscricaoAtleta` + `InscricaoEquipe`), `partida` (+ classificação). Shared error handling in `common/error/` (`ApiError`, `GlobalExceptionHandler`); `Clock` bean (America/Fortaleza) in `config/ClockConfig.java`.
- **Versionamento de API (nativo Spring 7 / Boot 4)**: `config/ApiVersioningConfig.java` implementa `WebMvcConfigurer.configureApiVersioning`, resolve a versão do 1º segmento do path via `usePathSegment(0, ...)`, suporta `1.0` e usa `1.0` como default. Os controllers usam `@RequestMapping(path = "/v{version}/...", version = "1.0")`. Paths fora do padrão `/vN/...` (ex.: `/error`) não são versionados.
- REST base paths (all v1): `/v1/atletas`, `/v1/categorias`, `/v1/clubes`, `/v1/competicoes`, `/v1/equipes`, `/v1/modalidades`, `/v1/inscricoes-atletas`, `/v1/inscricoes-equipes`, `/v1/users`, `/v1/partidas`; classificação em `/v1/competicoes/{id}/categorias/{catId}/classificacao`.
- **Filtro de status**: todas as listagens aceitam `?status=VALOR` (enum do recurso); omitido = ativos + inativos. `GET /{id}` continua retornando inativos.
- **Competição**: `dataInicioInscricao`/`dataFimInscricao` + `dataInicio`/`dataFim`; status é recalculado pelas datas (`CompeticaoStatusScheduler`, horário) e `PATCH /status` só aceita `CANCELADA`. Criação exige modalidade `ATIVO`.
- **Inscrições**: `PENDENTE/APROVADA/INATIVA` ocupam vaga; `CANCELADA/REJEITADA` liberam. Uma equipe só pode ter uma inscrição ativa por competição (checagem no service; **sem unique no banco**). Transições inválidas → 409. Cancelar/inativar equipe cascateia atletas e partidas. Inscrição exige atleta e categoria `ATIVO`; equipe só é aprovada com ≥1 atleta `PENDENTE/APROVADA`; não se remove o último atleta ativo de equipe aprovada. Reativações revalidam janela, equipe, categoria, modalidade e duplicidade.
- **Auth (versionado)**: `POST /v1/auth/register` (`username`, `email`, `password`; role sempre `USER`) e `POST /v1/auth/login` (`email`, `password`).
- **Rate limit de login (A5)**: `POST /v1/auth/login` consome dois buckets bucket4j em memória (`security/auth/LoginRateLimiter`): por IP (`max-por-ip`) e por (IP, e-mail normalizado) (`max-por-ip-email`). Excedeu → `429` + `Retry-After` no formato `ApiError`; sucesso devolve a tentativa. **Não é lockout**: não há `bloqueadoAte` nem migration. IP = `request.getRemoteAddr()` (sem confiar em `X-Forwarded-For`). Estado em Caffeine, por instância.
- **Módulo `security`**: `security/config` (`SecurityConfig`, `JwtAuthenticationFilter`, `Rest*`, `LoginRateLimitProperties`, `RateLimitConfig`, `ClockTimeMeter`), `security/jwt` (`JwtService`, `TokenProperties`), `security/auth` (`AuthController`, `AuthService`, `LoginRateLimiter`, `CustomUserDetailsService`, `UserRepository`), `security/users` (`User`, `UserRole`, `UserStatus`, `UserDetailsAdapter`, `UserController`, `UserService`, `UserMapper`, `UserSpecifications`, `UserAudit`, `UserAuditService`, `dto/*`).
- **Identidade**: email é único (`uk_db_users_email`, V13) e é o `sub` do JWT; username **não** é único (V14 adicionou, V15 removeu) e é apenas descritivo. A entidade `User` não implementa mais `UserDetails`; `UserDetailsAdapter` (record em `security/users`) embrulha a entidade e devolve **email** em `getUsername()`, então `Authentication.getName()` é o email.
- **Autorização** (`SecurityConfig`): `/v1/auth/**` público; `GET /v1/users/**` exige `ADMIN` ou `SUPERADMIN`; escrita em `/v1/users/**` e `GET /v1/users/audit` exige `SUPERADMIN`; `GET /v1/**` exige `USER` ou `ADMIN`; `POST`/`PATCH`/`PUT`/`DELETE /v1/**` exige `ADMIN`; demais requests exigem `USER`. `ADMIN` carrega `ROLE_ADMIN`+`ROLE_USER`; `SUPERADMIN` carrega `ROLE_SUPERADMIN`+`ROLE_ADMIN`+`ROLE_USER` (`UserDetailsAdapter.getAuthorities`).
- Atleta: `GET /?nome=&status=&page=&size=`, `GET /{id}`, `POST /`, `PUT /{id}`, `PATCH /{id}/status`. Paginated — `AtletaService.java:22` returns `Page`.
- **Users**: `GET /v1/users?search=&status=` e `GET /v1/users/{id}` (ADMIN/SUPERADMIN); `POST /v1/users`, `PUT /v1/users/{id}` (senha opcional) e `PATCH /v1/users/{id}/status` (soft delete) só `SUPERADMIN`; `GET /v1/users/audit` só `SUPERADMIN`.
- **Partidas**: `GET/POST /v1/partidas`, `GET/PUT /{id}`, `PATCH /{id}/status`, `PATCH /{id}/resultado`; referenciam `InscricaoEquipe` (mandante/visitante). Requer inscrições `APROVADA`, mesma competição/categoria, equipes distintas, data no período e uma equipe por rodada/horário. Cadastro é permitido em competição `FINALIZADA`; em competição `CANCELADA` a partida nasce `CANCELADA`. Filtros `competicaoId`/`categoriaId`/`status` são combináveis. Classificação inicia com inscrições `APROVADA` (0 jogos) e calcula 3/1/0 com desempate pontos → vitórias → saldo → gols pró → confronto direto → nome.
- **Regras de usuário**: `ADMIN` é somente leitura em `/v1/users`. `SUPERADMIN` não cria/promove `SUPERADMIN` pela API (só o seed da `V17`), não altera o próprio `role`/`status`, não edita/inativa outro `SUPERADMIN` e pode editar o próprio perfil (mantendo `role`).
- Inscrições de atleta têm `PUT /v1/inscricoes-atletas/{id}` para editar apenas `numeroCamisa`; inscrições de equipe são imutáveis (cancele e crie outra).
- **Exclusão física × lógica**: `DELETE /v1/atletas/{id}` é a **única** exclusão física (`AtletaService#hardDelete`): exige atleta `INATIVO` e sem inscrições `OCUPAM_VAGA`; apaga antes as inscrições `LIBERAM_VAGA` (`CANCELADA`/`REJEITADA`) e devolve `204`/`409`. Todos os demais domínios usam apenas descarte lógico por `status`.
- **`observacao` (V21)**: texto livre (`varchar(500)`, opcional) em `Competicao`, `InscricaoEquipe`, `InscricaoAtleta` e `Partida`. **Não tem vínculo com status** e não dispara nada: aceito no `POST`, editável/limpável em qualquer estado via `PATCH .../observacao` (`*ObservacaoRequest`). Fica fora de `atualizarDados(...)`, que bloqueia registros inativos/cancelados — é a via que ignora esses bloqueios de propósito. Ausente/em branco ⇒ `null`.
- **`dataCancelamento` (V21)**: informativa, gravada só na transição para `CANCELADA` (inclui o bulk `PartidaRepository.cancelarPor*`, que recebe o instante do `Clock`).
- DTOs are `record`s. Mapping via MapStruct `componentModel="spring"`.

## Gotchas — Won't Find Without Reading Code
- **CPF normalization**: `extras/CpfUtils.java` strips non-digits, called in `Atleta.java` `@PrePersist/@PreUpdate` AND `AtletaService.java:40` before saving. Entity has `@Pattern("\\d{11}")` (`Atleta.java:46`) which rejects formatted CPF before normalization — fix belongs in DTOs/service, not entity.
- **MapStruct + Lombok coupling**: generated mappers go to `target/generated-sources/annotations/`. After editing `AtletaMapper.java` run `./mvnw clean compile` to regenerate.
- **`version` em `@RequestMapping` + MockMvc standalone**: sem uma `ApiVersionStrategy`, o Spring lança `IllegalStateException: API version specified, but no ApiVersionStrategy configured` ao montar o handler. `AuthControllerTest` configura via `.setApiVersionStrategy(...)`; testes novos com controllers versionados precisam fazer o mesmo.
- **Dois níveis de erro de segurança**: `GlobalExceptionHandler` (`common/error/GlobalExceptionHandler.java`, `@RestControllerAdvice`) cobre erros do controller (validação, domínio e `AuthenticationException` do login → 401). Erros dos filtros (sem token/inválido → 401, role insuficiente → 403) são escritos por `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler` via `ApiErrorResponseWriter`. O `@RestControllerAdvice` não alcança exceções de filtro.
- **Jackson 3**: `ApiErrorResponseWriter` injeta `tools.jackson.databind.ObjectMapper` (bean do Boot 4.1). As anotações `com.fasterxml.jackson.annotation.*` continuam válidas; o que mudou de pacote foi o databind.
- **MySQL error code in service**: `AtletaService.java:48` inspects `SQLIntegrityConstraintViolationException.getErrorCode() == 1062` (duplicate CPF). This branch is not exercised under H2 (unit tests mock it).
- **Registro de usuário**: `AuthService.register` pré-checa `existsByEmail` e traduz `DataIntegrityViolationException` cujo `getMostSpecificCause().getMessage()` contém `uk_db_users_email` para `EmailJaCadastradoException` (409). Username duplicado é permitido por decisão.
- **Busca de usuário no auth**: `UserRepository.findByEmail` retorna `Optional<User>`; `CustomUserDetailsService` lança `UsernameNotFoundException` quando não encontra (o `DaoAuthenticationProvider` converte em `BadCredentialsException`, mantendo o 401 genérico).
- **Token inválido no filtro**: `JwtService.extractEmail` lança `TokenInvalidoException`; o `JwtAuthenticationFilter` captura e segue a request sem autenticação (sem 500/NPE). O status final é tratado pelo `RestAuthenticationEntryPoint` (sem token/inválido → 401 JSON) e pelo `RestAccessDeniedHandler` (role insuficiente → 403 JSON).
- **Usuário inativo**: `UserStatus` (ATIVO/INATIVO, coluna `status` criada na `V16`) é checado em `UserDetailsAdapter.isEnabled()`. O filtro JWT aplica `.filter(UserDetailsAdapter::isEnabled)`, então token já emitido de usuário inativado deixa de autenticar; o login também é bloqueado.
- **Soft delete**: é apenas `status` (`ATIVO/INATIVO`, ou `CANCELADA` em competição/inscrição). Não há `@SQLDelete`/`@Where`: listagens trazem inativos a menos que o cliente envie `?status=`; uniques (CPF, email, número de camisa) continuam ocupados.
- **Bloqueios de inativação**: atleta/equipe/categoria/clube/modalidade com vínculos ativos → 409 (`VinculoAtivoException`). Competição não bloqueia: `CANCELADA` cascateia inscrições e partidas.
- **Categoria**: duplicidade só por **faixa etária idêntica entre categorias ATIVAS** da mesma competição; nomes livres e sobreposição parcial permitidos.
- **CPF**: `@CPF` ativo no create; `GET /v1/atletas/cpf/{cpf}` normaliza com `CpfUtils`.
- **Bootstrap SUPERADMIN**: `V17__promote_seed_superadmin.sql` promove `admin@quixada.gov.br` (seed da `V11`) a `SUPERADMIN` e garante `status=ATIVO`. A senha do seed é definida na `V11` e **deve ser rotacionada no primeiro deploy** (o histórico de migrations é público); ver runbook em `docs/operacao/recuperacao-superadmin.md`.
- **Recuperação de senha do SUPERADMIN (exatamente 1, sem criação/promoção pela API)**: operação de **servidor**, sem endpoint HTTP. `SuperadminPasswordResetRunner` (`@Profile("superadmin-reset")`) lê e-mail (`--reset.email`/`SUPERADMIN_EMAIL`, default `admin@quixada.gov.br`) e senha (console interativo ou `SUPERADMIN_NEW_PASSWORD`), re-hasheia com BCrypt, reativa e audita (`PASSWORD_RESET`). Rodar: `java -jar target/gestao-esportiva-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev,superadmin-reset` (`application-superadmin-reset.yaml` usa porta efêmera e desliga Docker Compose). Runbook completo: `docs/operacao/recuperacao-superadmin.md`.
- **Auditoria de usuários**: `V18__create_user_audit.sql` cria `db_user_audit`; `UserService` grava `CREATE`/`UPDATE`/`STATUS_CHANGE` via `UserAuditService` (nunca registra senha). Leitura em `GET /v1/users/audit` (SUPERADMIN).
- **Política de senha**: aplicada em `RegisterRequest` (`@Size(min = 8, max = 72)`; BCrypt ignora bytes além de 72).
- **Rate limit por instância**: `LoginRateLimiter` usa Caffeine local. Restart zera contadores e N réplicas multiplicam o limite efetivo (N×); a evolução é trocar o `Cache<String,Bucket>` por um `ProxyManager` do bucket4j. A evicção do cache (`maximumSize`) recria buckets cheios (fail-open), então monitore o tamanho antes de reduzir `maximo-chaves`. Atrás de proxy, `getRemoteAddr()` devolve o proxy — configurar `server.forward-headers-strategy=framework`.
- **`@CreationTimestamp` Hibernate-specific** (`Atleta.java`) — tied to Hibernate, not Spring Data auditing. Tests need a full context.
- **Iteration notes**: `src/main/java/br/gov/quixada/esporte/iterations/pendencias.md` is the consolidated roadmap of everything not yet implemented/pending — check before large refactors. QA report: `docs/qa/relatorio-testes-unitarios.html` (tracked).
- **Git**: current work branch is `dev`; `main` recebe PRs de `dev` (PR #8 já mergeado). No CI / lint / formatter config, no `.github/`, no `opencode.json`. `HELP.md` is Spring Initializr boilerplate.
