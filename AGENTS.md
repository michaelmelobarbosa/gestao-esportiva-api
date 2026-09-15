# AGENTS.md

## Stack
- Java 25 + Spring Boot 4.1.0 + Maven wrapper (`./mvnw`, Maven 3.9.16). `pom.xml:16` pins `java.version=25` — ensure JDK 25 is active (`java -version`).
- Spring Data JPA + MySQL 8.4 (`mysql-connector-j` runtime) in `dev`; H2 in-memory (`com.h2database:h2`, test scope) in `test`.
- **Flyway is active** (`spring-boot-starter-flyway` + `flyway-mysql`, `pom.xml:44-51`). Migrations live in `src/main/resources/db/migration/` (`V1__baseline.sql` .. `V6__inscricao_constraints.sql`). `ddl-auto: validate` in dev — schema comes from migrations, not Hibernate. Never edit an applied migration (checksum breaks); add `V7__...`.
- MapStruct 1.6.3 + Lombok. Both must stay in `pom.xml:103-113` `maven-compiler-plugin.annotationProcessorPaths` — removing either breaks generated mappers.

## Environments / Profiles
Two Spring profiles; default is `dev` (`application.yaml:8-9`, overridable via `SPRING_PROFILES_ACTIVE`).
- **dev** (`application-dev.yaml`): MySQL `localhost:3306/gestao_esportiva`, credentials from `.env`, Flyway on, `ddl-auto: validate`.
- **test** (`application-test.yaml`): H2 in-memory `jdbc:h2:mem:gestao_esportiva_test;MODE=MySQL`, Flyway off, `ddl-auto: create-drop`, Spring Docker Compose off. Surefire forces it — `pom.xml:125-127` sets `spring.profiles.active=test`.
- H2 is test-scoped only. Tests no longer need Docker/MySQL: the service tests are pure Mockito, only `contextLoads` boots a context.

## Commands
```bash
cp .env.example .env  # fill MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE, MYSQL_ROOT_PASSWORD
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

## Architecture
- Entrypoint: `src/main/java/br/gov/quixada/esporte/GestaoEsportivaApiApplication.java`.
- All domains are now fully wired (entity → repository → service → controller → MapStruct mapper → DTO records): `atleta`, `categoria`, `clube`, `competicao`, `equipe`, `modalidade`, `inscricao` (`InscricaoAtleta` + `InscricaoEquipe`). Shared error handling in `common/error/` (`ApiError`, `GlobalExceptionHandler`); `Clock` bean (America/Fortaleza) in `config/ClockConfig.java`.
- REST base paths (all v1): `/v1/atletas`, `/v1/categorias`, `/v1/clubes`, `/v1/competicoes`, `/v1/equipes`, `/v1/modalidades`, `/v1/inscricoes-atletas`, `/v1/inscricoes-equipes`.
- Atleta: `GET /?nome=&page=&size=`, `GET /{id}`, `POST /`, `PUT /{id}`, `PATCH /{id}/status`. Paginated — `AtletaService.java:22` returns `Page`.
- DTOs are `record`s. Mapping via MapStruct `componentModel="spring"`.

## Gotchas — Won't Find Without Reading Code
- **CPF normalization**: `extras/CpfUtils.java` strips non-digits, called in `Atleta.java` `@PrePersist/@PreUpdate` AND `AtletaService.java:40` before saving. Entity has `@Pattern("\\d{11}")` (`Atleta.java:46`) which rejects formatted CPF before normalization — fix belongs in DTOs/service, not entity. See `iterations/melhorias-atleta.md`.
- **MapStruct + Lombok coupling**: generated mappers go to `target/generated-sources/annotations/`. After editing `AtletaMapper.java` run `./mvnw clean compile` to regenerate.
- **`GlobalExceptionHandler`** (`common/error/GlobalExceptionHandler.java`, `@RestControllerAdvice`) maps domain exceptions plus `MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException` and `DataIntegrityViolationException` to `ApiError`. No per-controller handling needed.
- **MySQL error code in service**: `AtletaService.java:48` inspects `SQLIntegrityConstraintViolationException.getErrorCode() == 1062` (duplicate CPF). This branch is not exercised under H2 (unit tests mock it).
- **`@CreationTimestamp` Hibernate-specific** (`Atleta.java`) — tied to Hibernate, not Spring Data auditing. Tests need a full context.
- **Iteration notes**: `src/main/java/br/gov/quixada/esporte/iterations/` (`melhorias-atleta.md`, `decisoes.md`, `futuras-iterações.md`, `estudos.md`) is the roadmap/decision record — check before large refactors. QA report: `docs/qa/relatorio-testes-unitarios.html` (untracked).
- **Git**: current work branch is `testes`; `main`/`dev` and feature branches exist remotely. No CI / lint / formatter config, no `.github/`, no `opencode.json`. `HELP.md` is Spring Initializr boilerplate.
