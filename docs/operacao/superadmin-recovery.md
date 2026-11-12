# SUPERADMIN password recovery

> **Server-side operation.** There is no HTTP endpoint for recovery. The reset is done with a command on the server terminal, which requires access to the database credentials and the application's environment variables.
>
> This document **contains no secrets**: the values below are placeholders. Never put real passwords here, the JWT secret, database credentials, or internal hostnames.

## Context

- The system keeps **exactly 1 SUPERADMIN**, responsible for managing the `ADMIN` users.
- The API does not allow creating or promoting another `SUPERADMIN`.
- Recovery is performed by `SuperadminPasswordResetRunner` (profile `superadmin-reset`), which:
  1. validates the informed email and requires the user to have the `SUPERADMIN` role;
  2. re-hashes the new password with BCrypt;
  3. reactivates the account (`status = ACTIVE`);
  4. records the `PASSWORD_RESET` audit entry (actor `system:cli`);
  5. exits the process with code `0` (success) or `1` (failure).

## Prerequisites

- JDK 25 (`java -version`).
- Reachable database.
- The same variables required for the application's normal boot:
  - `API_SECURITY_TOKEN_SECRET` (minimum 32 characters);
  - database credentials (`DB_URL`/`DB_USER`/`DB_PASSWORD` in the `prod` profile, or `SPRING_DATASOURCE_*`).
- Shell access permission on the server.

## 1. Build the artifact

```bash
./mvnw package -DskipTests
```

The jar is generated at `target/sports-management-api-0.0.1-SNAPSHOT.jar`.

## 2. Interactive mode (recommended)

```bash
java -jar target/sports-management-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod,superadmin-reset
```

The terminal asks for the new password (hidden input):

```
New SUPERADMIN password:
Confirm the new password:
```

At the end:

```
SUPERADMIN password 'admin@quixada.gov.br' reset successfully.
```

The process exits on its own. `0` = success; `1` = failure.

## 3. Non-interactive mode

For automation or environments without an interactive console:

```bash
SUPERADMIN_NEW_PASSWORD='<NEW_PASSWORD>' \
  java -jar target/sports-management-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod,superadmin-reset
```

> If a console is available, the value of `SUPERADMIN_NEW_PASSWORD` is ignored (the console takes precedence). **Never** pass the password as a command-line argument (`--password=...`), because it would show up in `ps` and in the shell history.

## 4. Target email

The default is the seed email. To use another email:

```bash
# via argument
java -jar target/sports-management-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod,superadmin-reset \
  --reset.email=<SUPERADMIN_EMAIL>

# or via environment variable
SUPERADMIN_EMAIL=<SUPERADMIN_EMAIL> \
  java -jar target/sports-management-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod,superadmin-reset
```

Precedence: `--reset.email` > `SUPERADMIN_EMAIL` > default seed email.
The informed email must exist and have the `SUPERADMIN` role, otherwise the command fails.

## 5. Production

The `prod` profile is defined in `application-prod.yaml`, which requires the environment variables
`DB_URL`, `DB_USER`, `DB_PASSWORD`, `API_CORS_ALLOWED_ORIGINS` and `API_SECURITY_TOKEN_SECRET`
(the boot fails without them). Example with placeholders:

```bash
SPRING_PROFILES_ACTIVE=prod,superadmin-reset \
DB_URL='jdbc:mysql://<DB_HOST>:3306/sports_management?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=America/Recife' \
DB_USER='<DB_USER>' \
DB_PASSWORD='<DB_PASSWORD>' \
API_CORS_ALLOWED_ORIGINS='<CORS_ORIGINS>' \
API_SECURITY_TOKEN_SECRET='<JWT_SECRET>' \
  java -jar /path/to/sports-management-api-0.0.1-SNAPSHOT.jar
```

The `superadmin-reset` profile uses an ephemeral port (`server.port: 0`) and disables Docker Compose; the process starts, performs the reset and exits.

## 6. After the reset

Log in normally and confirm the access:

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<SUPERADMIN_EMAIL>","password":"<NEW_PASSWORD>"}'
```

The action is recorded in `db_user_audit` with `action = PASSWORD_RESET`, actor `system:cli` and the reset target. It can be queried at `GET /v1/users/audit` (requires a `SUPERADMIN` token).

## Limitations

- **JWT is stateless:** tokens issued before the reset remain valid until they expire (default `2h`). Immediate revocation is not implemented (item C6 of the roadmap).
- The command **does not create** a user. If there is no `SUPERADMIN` with the informed email, it fails.
- The reset **reactivates** the account, even if it was inactive.

## Best practices

- Rotate the `SUPERADMIN` password immediately after the first deploy (the seed is public in the migration history).
- Restrict shell access on the server to the smallest possible number of operators.
- Review the user audit periodically.
- Do not log the password or the JWT secret in logs, tickets, or shell history.

## Troubleshooting

| Message | Cause |
|---|---|
| `Failed to reset password: Password must be between 8 and 72 characters` | Password outside the allowed range |
| `Failed to reset password: SUPERADMIN not found for the informed email` | Non-existent email or user without the `SUPERADMIN` role |
| `Password not provided. Use the interactive console or the SUPERADMIN_NEW_PASSWORD.` | No interactive console and no environment variable set |
| `Passwords do not match.` | Confirmation differs from the typed password |
| Connection failure / Flyway | Unreachable database or incorrect credentials/environment variables |

## Never include in this document

- The seed default password or any real password.
- The value of `API_SECURITY_TOKEN_SECRET`.
- Database users/passwords.
- Production hostnames, IPs, or internal URLs.
