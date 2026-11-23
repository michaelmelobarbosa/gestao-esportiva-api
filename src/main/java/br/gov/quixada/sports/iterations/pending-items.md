# Pending Items — Sports Management API

> File consolidated on 2026-11-09; updated on 2026-11-16 to mark completed items. It gathers **everything that has not yet been implemented or is pending** in the project, extracted from `decisions.md`, `studies.md`, `fixes.md`, `future-iterations.md` and `securityfixes.md` (removed after consolidation).
>
> Completed items remain marked with `[x]` for traceability; pending ones stay `[ ]`.
>
> Priorities: **P0** critical/exploitable · **P1** important · **P2** improvement/good practice.

---

## Checklist

### Security / Authentication
- [x] **A5** — Rate limiting on login (bucket4j, no lockout) (P0)
- [x] **C6** — Refresh token / logout / revocation (P2)
- [x] **C7** — (Optional) roles as a JWT claim (P2) — role claim added as informational while retaining DB lookup for immediate revocation via tokenVersion & status
- [x] **C8** — Configure CORS (P2)

### Users / Governance
- [x] Decision: exactly **1 SUPERADMIN**, no creation/promotion through the API (existing blocks in `UserService`)
- [x] SUPERADMIN password recovery via CLI (P1)
- [x] Audit: filters by actor/target/period, retention/purge and export (P2)

### Athlete
- [x] `hardDelete` + `DELETE /v1/athletes/{id}` endpoint (P1)
- [x] `AthleteControllerTest` (P1)
- [x] `AthleteMapperTest` (P1)
- [x] Configure standalone `MockMvc` + fixed `Clock` in controller tests (P1)

### Tests for the new flows
- [x] Controller/mapper tests for competition, registration, match and standings (P1)

### Sports core
- [x] Automatic round generation (single round-robin / double round-robin) (P1)
- [x] Minimum/maximum roster size (P1) — configurable min/max on Category (default min = 1, max = unlimited); sport-specific presets remain technical staff configuration choice

### Infrastructure / Documentation
- [x] OpenAPI/springdoc + `@Tag`/`@Operation` (P2)
- [x] README + versioning strategy documentation (P2)
- [x] `application-prod.yaml` + separate dev/prod config (P2)
- [x] Verify Java 25 + Spring Boot 4.1.0 + MapStruct 1.6.3 compatibility (P2)
- [x] CI (GitHub Actions) running `./mvnw verify` (P2)
- [x] Testcontainers (optional) (P2)

---

## Details

### A5 — Rate limiting on login (P0) — Completed
- **Where:** `security/auth/AuthController#login`, `security/auth/LoginRateLimiter`, `security/config/LoginRateLimitProperties`, `security/config/ClockTimeMeter`, `security/config/RateLimitConfig`, `security/auth/exception/TooManyAttemptsException`, `common/error/GlobalExceptionHandler`.
- **Previous situation:** unlimited attempts; `UserDetailsAdapter.isAccountNonLocked()` always returned `true`; there was no `bucket4j`/`Resilience4j` in the `pom.xml`.
- **Decision:** **rate limiting, not account lockout**, with **bucket4j core** (`bucket4j_jdk17-core:8.22.0`, explicit version) + **Caffeine** (version managed by Boot) to bound memory. No migration and no persistent state. `Resilience4j` stays reserved for future external calls (retry/circuit breaker).
- **Model:** two in-memory buckets per attempt — per **IP** (`max-per-ip`) and per **(IP, normalized email)** (`max-per-ip-email`). Never by email alone: it would allow locking out third parties' accounts (DoS). Consumption happens **before** BCrypt; a successful login returns the attempt (`addTokens`). When exceeded → `429` in the `ApiError` format + `Retry-After` header (seconds, from `getNanosToWaitForRefill`), with a generic message.
- **Config:** `api.security.login-rate-limit.{max-per-ip:100, max-per-ip-email:5, window:15m, max-keys:100000}`. In prod, tunable via `API_LOGIN_RATE_LIMIT_*` (with defaults).
- **Semantics:** `refillGreedy` ⇒ 5 attempts/15 min = 1 token every 3 min. It is soft throttling, not a hard 15-minute block.
- **Limitations:** state is in memory and per instance — N replicas ⇒ effective limit N×; a restart resets counters; Caffeine eviction (`max-keys`) recreates full buckets (fail-open); the evolution is to swap the `Cache<String,Bucket>` for a `ProxyManager` (Redis/MySQL). The IP comes from `request.getRemoteAddr()`; behind a trusted proxy, configure `server.forward-headers-strategy` (otherwise all traffic appears to come from the proxy IP).
- **Tests:** `LoginRateLimiterTest` (8), `AuthControllerLoginRateLimitTest` (4) and `LoginRateLimitPropertiesTest` (3), with no `Thread.sleep` (`ClockTimeMeter` + `MutableClock`; IP via `request.setRemoteAddr`).

### C6 — Refresh token / logout / revocation (P2) — Completed
- **Where:** `security/jwt/RefreshToken`, `RefreshTokenRepository`, `RefreshTokenService`, `RefreshTokenRequest`, `TokenProperties`, `security/auth/AuthController#login`, `#refresh`, `#logout`, `security/config/JwtAuthenticationFilter`, Flyway migration `V26__create_refresh_tokens_and_token_version.sql`.
- **How it turned out:** `POST /v1/auth/login` returns `{ token, refreshToken }`. Refresh tokens are opaque, securely generated tokens stored as SHA-256 hashes in `db_refresh_tokens`.
- **Rotation & Reuse Detection:** `POST /v1/auth/refresh` rotates the token (marks the existing token as revoked and issues a new pair). If a revoked token is presented (reuse attempt), the entire token family is revoked and `db_users.token_version` is incremented.
- **Revocation & Logout:** `POST /v1/auth/logout` (authenticated) revokes all refresh tokens for the user and increments `token_version`. Changing password, updating user role, deactivating an account, or performing CLI SUPERADMIN password recovery also increments `token_version`, instantly invalidating all active JWT access tokens on subsequent requests.
- **Tests:** `RefreshTokenServiceTest`, `AuthControllerTest`, and `MySqlFlywayAndConstraintsTest`.

### C7 — Roles as a JWT claim (P2) — Completed
- **Where:** `security/jwt/JwtService`, `security/config/JwtAuthenticationFilter`.
- **Architectural Decision:** `role` claim was added to the JWT payload as informational data (useful for client-side decoding and observability). However, the architecture intentionally retains the database user lookup in `JwtAuthenticationFilter` (`userRepository.findByEmail`).
- **Rationale:** Retaining the DB lookup guarantees immediate revocation: checking `user.getTokenVersion() == claims.tokenVersion()` and `user.isEnabled()` ensures that token version bumps (logout, password change, deactivation, role change) and account deactivations take effect immediately without having to wait for the 2-hour JWT access token expiration window.

### C8 — Configure CORS (P2) — Completed
- **Where:** `security/config/CorsProperties`, `SecurityConfig`, `application.yaml` / `application-prod.yaml`.
- **How it turned out:** `api.cors.allowed-origins` (list, via `API_CORS_ALLOWED_ORIGINS`) → `CorsConfigurationSource` bean registered on `/**` and applied by `.cors(...)` in the `SecurityFilterChain`. Methods `GET/POST/PUT/PATCH/DELETE/OPTIONS`; headers `Authorization` and `Content-Type`; `Location` exposed; `maxAge` 1h.
- **Behavior:** preflight from a non-listed origin → `403`; listed origin → `200` with `Access-Control-Allow-Origin`.
- **Tests:** `CorsConfigTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`) covers both an allowed origin and an intruder origin.
- **Attention in prod:** the boot fails if `API_CORS_ALLOWED_ORIGINS` is not provided.

### SUPERADMIN password recovery via CLI (P1) — Completed
- **Status:** implemented in commit `f97544d`. Runbook at `docs/operacao/superadmin-recovery.md`.
- **Decision:** the system keeps **exactly 1 SUPERADMIN**, responsible for managing the `ADMIN` users. The API still blocks creating/promoting a `SUPERADMIN` (`UserService`) and there is no recovery endpoint exposed on the network.
- **Where:** `security/users/SuperadminPasswordResetService`, `security/users/cli/SuperadminPasswordResetRunner`, profile `superadmin-reset` (`application-superadmin-reset.yaml`).
- **Why:** with no email/MFA, the real risk is losing the password of the single SUPERADMIN. Recovery requires shell access on the server — the same privilege level that would already allow changing the database — so it adds no remote attack surface.
- **How it works:** `java -jar app.jar --spring.profiles.active=prod,superadmin-reset` (or `dev,superadmin-reset`); the runner reads the email (`--reset.email`/`SUPERADMIN_EMAIL`, default `admin@quixada.gov.br`) and the password (interactive console or `SUPERADMIN_NEW_PASSWORD`), re-hashes with BCrypt, reactivates the account and audits `PASSWORD_RESET`, then exits.
- **Limitation:** JWT is stateless — tokens issued before the reset remain valid until they expire (2h); immediate revocation depends on item C6.
- **How to test:** `SuperadminPasswordResetServiceTest` covers success, non-existent email, non-SUPERADMIN user and out-of-range password.

### User audit: filters, retention/purge and export (P2) — Completed
- **Where:** `security/users/UserController#findAudit` (`GET /v1/users/audit`), `UserController#exportAudit` (`GET /v1/users/audit/export`), `UserAuditSpecifications`, `UserAuditService`, `UserAuditRetentionScheduler`, `UserAuditRetentionProperties`, `UserAuditRepository`, table `db_user_audit` (`V18`).
- **How it turned out:**
  - Combinable filters implemented via Spring Data JPA `Specification`: `actorEmail`, `targetEmail`, `action`, `from`, `to` datetime range with pagination.
  - Export endpoint: `GET /v1/users/audit/export?format=csv|json` allows SUPERADMIN to export filtered audit records (up to `api.audit.export-max-rows`, default 10,000). CSV export implements formula injection neutralization (sanitizing cells starting with `=`, `+`, `-`, `@`, `\t`, `\r`).
  - Retention & Purge: `UserAuditRetentionScheduler` runs daily at 03:00 (configured via `api.audit.purge-cron` and `api.audit.retention-days`, default 365 days; configurable in prod via `API_AUDIT_RETENTION_DAYS`) and deletes audit entries older than the retention threshold.
- **Tests:** `UserAuditServiceTest`, `UserAuditSpecificationsTest`, `UserControllerTest`.

### Athlete `hardDelete` + endpoint (P1) — Completed
- **Status:** implemented. `AthleteService#hardDelete` + `DELETE /v1/athletes/{id}`.
- **Team decision:** physical deletion **only for athletes**. The other domains are not deleted: they use `status` (`INACTIVE`/`CANCELED`) plus a free-text `notes` field to record any information (cancellation reason, date extension, announcements).
- **Rule:** requires the athlete to be `INACTIVE` (otherwise `409`) and no registration in `OCCUPY_SLOT` (`PENDING`/`APPROVED`/`INACTIVE`). Registrations in `FREE_SLOT` (`CANCELED`/`REJECTED`) are deleted along with it. Returns `204`; `404` if it does not exist.
- **Verified impact:** `db_athletes` is only referenced by `db_athlete_registration`; standings do not use athlete registrations and there is no athlete audit. Accepted side effect: the CPF unique constraint is released.
- **Tests:** `AthleteServiceTest` (nested `hardDelete`) and `AthleteHardDeleteRepositoryTest` (the FK blocks the delete when a registration exists; `deleteByAthleteIdAndStatusIn` frees the path).

### `notes` field + `canceled_at` (V21) — Completed
- **Team decision:** instead of physically deleting competition/registration/match, record the reason/context in a free-text field.
- **`notes`:** free text (`varchar(500)`, optional) on `Competition`, `TeamRegistration`, `AthleteRegistration` and `Match`. **Not tied to status** — it can be set on `POST`, edited or cleared at any time via `PATCH /v1/{resource}/{id}/notes` (the `*NotesRequest` records), including on `CANCELED`/`COMPLETED`/`INACTIVE` records. Absent/blank ⇒ `null`. It is not propagated in cascade.
- **`canceledAt`:** informational, written only on the transition to `CANCELED` (including the bulk update of matches).
- **Not in listings:** the `*SummaryResponse` records do not expose `notes`.

### `AthleteControllerTest` (P1) — Completed
- **Where:** `src/test/java/.../athlete/AthleteControllerTest.java` — 16 tests in `@Nested` groups (`POST`, `GET`, `PATCH /status`, `PUT`, `DELETE`, `versioning`).
- **Scenarios:** valid `POST` → `201` + `Location: http://localhost/v1/athletes/1`; blank name and invalid CPF check digit → `400` with `errors[].field`; duplicate CPF → `409`; `GET /{id}` and `GET /cpf/{cpf}` → `200`; `GET /v1/athletes/-1` and `GET /cpf/abc` → `400`; `PATCH /status` activate/deactivate → `200`, missing status → `400`; `PUT` → `200`, on an inactive athlete → `409`; `DELETE` → `204`, blocked → `409`; unsupported version → `400`.
- **Gotcha found:** the controller is `@Validated` ([AthleteController.java:22](AthleteController.java)), so Spring MVC **skips** its built-in method validation and delegates the parameter constraints (`@Positive`, `@Pattern`) to the AOP proxy that `MethodValidationPostProcessor` creates. Standalone MockMvc has no such proxy, so `GET /v1/athletes/-1` and `GET /cpf/abc` returned `200` (the service was called with the invalid value). Fixed by adding `ControllerTestSupport.mockMvcValidated(...)`, which wraps the controller in a `ProxyFactory` + `MethodValidationInterceptor`; the resulting `ConstraintViolationException` is mapped to `400` by `GlobalExceptionHandler`.
- **Note:** `verifyNoInteractions(service)` guards the validation paths, so the test fails if the proxy is dropped.

### `AthleteMapperTest` (P1) — Completed
- **Where:** `src/test/java/.../athlete/AthleteMapperTest.java`.
- **How it turned out:** instantiates `AthleteMapperImpl` directly and injects `AddressMapperImpl` via `ReflectionTestUtils` (the generated impl uses field injection). 9 cases: `calculateAge` with `Clock.fixed` (including the day before the birthday and null dates); `toEntity(AthleteUpdateRequest)` does not map `cpf`, `id`, `createdAt` nor `status`; `toGetResponse`/`toSummary` calculate the age and return `null` when `birthDate` is null.

### Configure standalone `MockMvc` + fixed `Clock` in controller tests (P1) — Completed
- **Where:** `src/test/java/br/gov/quixada/sports/support/ControllerTestSupport.java`, `config/ClockTestConfig.java`.
- **How it turned out:** `ControllerTestSupport` centralizes the standalone MockMvc (with `GlobalExceptionHandler` + `ApiVersionStrategy`) and exposes `FIXED_CLOCK` (`2026-06-15T12:00:00Z`, `America/Fortaleza`). `ClockTestConfig` is a `@TestConfiguration` with `@Primary` to override the `Clock` bean in tests that boot the context (`@Import(ClockTestConfig.class)`).
- **Adoption:** `AuthControllerTest` and `CompetitionControllerTest` now use `ControllerTestSupport.mockMvc(...)`, removing the duplicated versioning strategy. `AthleteControllerClockTest` validates the age calculated with the fixed clock on `GET` and `POST`; `ClockTestConfigTest` guarantees the fixed bean takes precedence.

### Controller/mapper tests for the new flows (P1) — Completed
- **Where:** `src/test/java/br/gov/quixada/sports/` — added standalone contract tests (`ControllerTestSupport.mockMvcValidated` + `PageableHandlerMethodArgumentResolver` + `AuthenticationPrincipalArgumentResolver`) and MapStruct unit tests across all remaining controllers and mappers:
  - `competition`: `CompetitionControllerTest`, `CompetitionMapperTest`
  - `registration`: `TeamRegistrationControllerTest`, `TeamRegistrationMapperTest`, `AthleteRegistrationControllerTest`, `AthleteRegistrationMapperTest`
  - `match`: `MatchControllerTest`, `MatchMapperTest`, `StandingsControllerTest`
  - `category` / `club` / `team` / `modality` / `users` / `shared`: `CategoryControllerTest`, `CategoryMapperTest`, `ClubControllerTest`, `ClubMapperTest`, `TeamControllerTest`, `TeamMapperTest`, `ModalityControllerTest`, `ModalityMapperTest`, `UserControllerTest`, `UserMapperTest`, `AddressMapperTest`.

### Automatic round generation (single round-robin / double round-robin) (P1) — Completed
- **Where:** `match/RoundRobinScheduler`, `match/MatchScheduleController#generate` (`POST /v1/competitions/{competitionId}/categories/{categoryId}/matches/generate`), `MatchService#generateSchedule`, `MatchScheduleGenerateRequest`, `MatchScheduleGenerateResponse`, Flyway migration `V24__make_match_scheduled_at_nullable.sql`.
- **How it turned out:**
  - Implements the polygon/circle round-robin scheduling algorithm for `APPROVED` team registrations within a category. Odd team counts automatically receive a dummy "bye" slot.
  - Supports both single and double round-robin (`doubleRound: true|false`). In double round-robin, matchups repeat with inverted home/away venues.
  - `V24__make_match_scheduled_at_nullable.sql` makes `db_match.scheduled_at` nullable so fixtures can be generated before dates and venues are decided. Transitioning a match to `IN_PROGRESS` validates that `scheduledAt != null`.
- **Tests:** `RoundRobinSchedulerTest`, `MatchScheduleControllerTest`, `MatchServiceTest`.

### Minimum/maximum roster size (P1) — Completed
- **Where:** `category/Category` (`minAthletes`, `maxAthletes`), `CategoryCreateRequest`, `CategoryUpdateRequest`, `CategoryResponse`, `CategorySummaryResponse`, `TeamRegistrationService`, `AthleteRegistrationService`, Flyway migration `V25__category_roster_limits.sql`.
- **Architectural Decision:** Configurable roster limits were placed on the `Category` entity (`min_athletes`, `max_athletes` in `db_category`), with `min >= 1` and `min <= max`.
- **Enforcement:**
  - `TeamRegistrationService` enforces that approving a team registration requires at least `category.effectiveMinAthletes()` (defaults to 1 if `minAthletes` is null).
  - `AthleteRegistrationService` enforces that adding an active athlete does not exceed `maxAthletes` (when configured), and that removing an athlete does not leave an approved team below `effectiveMinAthletes()`.
  - Modality/sport-specific defaults (e.g. 11 for soccer, 5 for futsal) remain a configuration choice made by technical staff when creating the category.
- **Tests:** `CategoryServiceTest`, `TeamRegistrationServiceTest`, `AthleteRegistrationServiceTest`, `CategoryControllerTest`.

### OpenAPI/springdoc + annotations (P2) — Completed
- **Where:** `pom.xml` (`springdoc-openapi-starter-webmvc-ui` 3.2.1), `config/OpenApiConfig`, `config/ApiVersioningConfig`, `security/config/SecurityConfig`, `application.yaml`, `application-prod.yaml`.
- **How it turned out:**
  - Swagger UI accessible at `/swagger-ui.html` and OpenAPI 3 schema at `/v3/api-docs`.
  - Configured with Bearer JWT security scheme in `OpenApiConfig`.
  - `ApiVersioningConfig` exempts `/v3/api-docs` and `/swagger-ui` from the path versioning segment predicate.
  - Excluded from CSRF and permitted for public GET access in `SecurityConfig`.
  - Controlled by `API_OPENAPI_ENABLED` (enabled by default in dev/test, disabled by default in `prod`).
- **Tests:** `OpenApiTest`.

### README + versioning strategy documentation (P2) — Completed
- **Status:** `README.md` created, with setup, authentication/roles, endpoint table, tests and the versioning decision (URL `/v1`; header-based alternative discarded; deprecation policy).
- **Note:** native versioning was already implemented (`/v{version}/...`, `version = "1.0"`); only the documentation was missing, now published.

### `application-prod.yaml` + separate dev/prod config (P2) — Completed
- **How it turned out:** `application.yaml` no longer enables `show-sql`/`format_sql` (now only in `dev`); `application-dev.yaml` gained `show-sql: true`, `format_sql` and `spring.docker.compose.enabled: true`; `application-prod.yaml` has `show-sql: false`, `format_sql: false`, `ddl-auto: validate`, Flyway on, Docker Compose off, configurable `server.port` and log levels.
- **Variables required in prod:** `DB_URL`, `DB_USER`, `DB_PASSWORD`, `API_CORS_ALLOWED_ORIGINS`, `API_SECURITY_TOKEN_SECRET` (the boot fails without them).

### Verify Java 25 + Boot 4.1.0 + MapStruct 1.6.3 compatibility (P2) — Completed
- **Result:** `./mvnw clean compile` with JDK 25.0.4.1 generates all `*MapperImpl` normally. The check **found and fixed** two `Unmapped target property: "normalizedFullName"` warnings in `AthleteMapper` (added `@Mapping(ignore = true)` on both `toEntity` methods). The build no longer emits any `[WARNING]` from the compiler.
- **Remaining issue:** JDK 25 prints the `sun.misc.Unsafe::objectFieldOffset ... terminally deprecated` warning triggered by `lombok.permit.Permit` (Lombok 1.18.46, already the latest version). It is harmless and has no fix on the project side; track it until Lombok ships an implementation without `Unsafe`.

### CI (GitHub Actions) (P2) — Completed
- **Where:** `.github/workflows/ci.yml`.
- **How it turned out:** workflow triggered on `push`/`pull_request` to `dev` and `main`, with JDK 25 (Temurin), Maven cache and `./mvnw --batch-mode --no-transfer-progress verify` (profile `test`/H2, no MySQL). `permissions: contents: read` and per-ref `concurrency`.

### Testcontainers (optional) (P2) — Completed
- **Where:** `support/AbstractMySqlIntegrationTest`, `integration/MySqlFlywayAndConstraintsTest`, `pom.xml` (`testcontainers:2.0.5` + `mysql:2.0.5`).
- **How it turned out:** `AbstractMySqlIntegrationTest` configures Testcontainers MySQL 8.4 via `@Testcontainers(disabledWithoutDocker = true)` and `@ServiceConnection`, running against Flyway migrations with `ddl-auto: validate`.
- **Tests covered:** Real MySQL duplicate CPF handling (1062 error code in `AthleteService`), functional unique indexes on `db_athlete_registration` (V23), seed data validation (V17 superadmin), and token hash uniqueness & rotation in MySQL. Tests skip silently when the Docker daemon is not active.
- **V22 fix verified:** Running `MySqlFlywayAndConstraintsTest` against a real MySQL 8.4 container uncovered and confirmed a pre-existing bug in `V22__rename_schema_to_english.sql:119` (`modify column gender` instead of `change column sexo gender`). Fixed with user authorization (`change column sexo gender`), and all 26 migrations (`V1`–`V26`) plus Hibernate `ddl-auto: validate` pass cleanly against MySQL 8.4.

### V23 conditional unique constraints & en-US hygiene — Completed
- **Where:** Flyway migration `V23__conditional_unique_athlete_registration.sql`.
- **Details:** Created MySQL 8.4 functional unique indexes (`uk_athlete_registration_jersey_active`, `uk_athlete_registration_athlete_active`) using expressions `(case when status in ('PENDING', 'APPROVED', 'INACTIVE') then 1 else null end)`. This ensures athlete and jersey number uniqueness only apply while occupying slots, allowing immediate reuse of numbers and athletes once prior registrations are `CANCELED` or `REJECTED`.

---

## Blockers / decisions required before implementing
- None remaining for the current iteration roadmap. All P0, P1, and P2 pending items have been implemented and verified.
