# AGENTS.md

## Stack
- Java 25 + Spring Boot 4.1.0 + Maven wrapper (`./mvnw`, Maven 3.9.16). `pom.xml:16` pins `java.version=25` — ensure JDK 25 is active (`java -version`).
- Spring Data JPA + MySQL 8.4 (`mysql-connector-j` runtime) in `dev`; H2 in-memory (`com.h2database:h2`, test scope) in `test`.
- **Flyway is active** (`spring-boot-starter-flyway` + `flyway-mysql`, `pom.xml:44-51`). Migrations live in `src/main/resources/db/migration/` (`V1__baseline.sql` .. `V15__drop_unique_users_username.sql`). `ddl-auto: validate` in dev — schema comes from migrations, not Hibernate. Never edit an applied migration (checksum breaks); add `V16__...`.
- MapStruct 1.6.3 + Lombok. Both must stay in `pom.xml:103-113` `maven-compiler-plugin.annotationProcessorPaths` — removing either breaks generated mappers.

## Environments / Profiles
Two Spring profiles; default is `dev` (`application.yaml:8-9`, overridable via `SPRING_PROFILES_ACTIVE`).
- **dev** (`application-dev.yaml`): MySQL `localhost:3306/gestao_esportiva`, credentials from `.env`, Flyway on, `ddl-auto: validate`.
- **test** (`application-test.yaml`): H2 in-memory `jdbc:h2:mem:gestao_esportiva_test;MODE=MySQL`, Flyway off, `ddl-auto: create-drop`, Spring Docker Compose off. Surefire forces it — `pom.xml:125-127` sets `spring.profiles.active=test`.
- H2 is test-scoped only. Tests no longer need Docker/MySQL: service tests are pure Mockito; `AtletaRepositoryTest` uses `@DataJpaTest` and `contextLoads` boots the full context (both on H2).

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
- All domains are now fully wired (entity → repository → service → controller → MapStruct mapper → DTO records): `atleta`, `categoria`, `clube`, `competicao`, `equipe`, `modalidade`, `inscricao` (`InscricaoAtleta` + `InscricaoEquipe`). Shared error handling in `common/error/` (`ApiError`, `GlobalExceptionHandler`); `Clock` bean (America/Fortaleza) in `config/ClockConfig.java`.
- **Versionamento de API (nativo Spring 7 / Boot 4)**: `config/ApiVersioningConfig.java` implementa `WebMvcConfigurer.configureApiVersioning`, resolve a versão do 1º segmento do path via `usePathSegment(0, ...)`, suporta `1.0` e usa `1.0` como default. Os controllers usam `@RequestMapping(path = "/v{version}/...", version = "1.0")`. Paths fora do padrão `/vN/...` (ex.: `/error`) não são versionados.
- REST base paths (all v1): `/v1/atletas`, `/v1/categorias`, `/v1/clubes`, `/v1/competicoes`, `/v1/equipes`, `/v1/modalidades`, `/v1/inscricoes-atletas`, `/v1/inscricoes-equipes`.
- **Auth (versionado)**: `POST /v1/auth/register` (`username`, `email`, `password`; role sempre `USER`) e `POST /v1/auth/login` (`email`, `password`).
- **Módulo `security`**: `security/config` (`SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`), `security/auth` (`AuthController`, `AuthService`, `CustomUserDetailsService`, `UserRepository`), `security/users` (`User`, `UserRole`, `dto/*`).
- **Identidade**: email é único (`uk_db_users_email`, V13) e é o `sub` do JWT; username **não** é único (V14 adicionou, V15 removeu) e é apenas descritivo. A entidade `User` não implementa mais `UserDetails`; `UserDetailsAdapter` (record em `security/users`) embrulha a entidade e devolve **email** em `getUsername()`, então `Authentication.getName()` é o email.
- **Autorização** (`SecurityConfig`): `/v1/auth/**` público; `GET /v1/**` exige `USER` ou `ADMIN`; `POST`/`PATCH`/`PUT`/`DELETE /v1/**` exige `ADMIN`; demais requests exigem `USER`. `ADMIN` também carrega `ROLE_USER` (`UserDetailsAdapter.getAuthorities`).
- Atleta: `GET /?nome=&page=&size=`, `GET /{id}`, `POST /`, `PUT /{id}`, `PATCH /{id}/status`. Paginated — `AtletaService.java:22` returns `Page`.
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
- **Política de senha**: aplicada em `RegisterRequest` (`@Size(min = 8, max = 72)`; BCrypt ignora bytes além de 72).
- **`@CreationTimestamp` Hibernate-specific** (`Atleta.java`) — tied to Hibernate, not Spring Data auditing. Tests need a full context.
- **Iteration notes**: `src/main/java/br/gov/quixada/esporte/iterations/` (`decisoes.md`, `futuras-iterações.md`, `estudos.md`, `securityfixes.md`, `fixes.md`) is the roadmap/decision record — check before large refactors. QA report: `docs/qa/relatorio-testes-unitarios.html` (tracked).
- **Git**: current work branch is `dev`; `main` recebe PRs de `dev` (PR #8 já mergeado). No CI / lint / formatter config, no `.github/`, no `opencode.json`. `HELP.md` is Spring Initializr boilerplate.
