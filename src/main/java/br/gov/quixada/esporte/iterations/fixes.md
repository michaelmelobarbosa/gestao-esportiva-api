# Fixes — Segurança e Autenticação

> **Documento de estudo.** Atualizado em 2026-11-05.
> Este arquivo consolida **o que já foi corrigido**, **o que ainda falta** e **por que cada decisão foi tomada**.
> O checklist detalhado item por item continua em `securityfixes.md`; o roadmap de features em `futuras-iterações.md`.

---

## 1. Objetivo

A autenticação da API funciona no fluxo feliz (registrar, logar, receber JWT, acessar rotas protegidas), mas a implementação inicial era "de tutorial": sem validação de entrada, segredo previsível, erros virando 500 e ausência de testes. Este documento registra a evolução dessa correção, em linguagem acessível, para servir de material de revisão.

O trabalho foi organizado em **batches** pequenos para facilitar revisão e aprendizado:

| Batch | Escopo | Status |
|---|---|---|
| 1 | Segredo obrigatório + robustez do token (`A2`, `B1`, `B5`, `B6`) | ✅ Concluído |
| 2 | Repositório, validação e contrato HTTP (`B7`, `B4`, `A4`, `B2`) | ✅ Concluído |
| 3 | Configuração e testabilidade do JWT (`C1`, `C2`, `C3`) | ✅ Concluído |
| 4 | Decisões maiores (rate limit, refresh/logout, CORS); versionamento (`C10`) concluído | ⏳ Pendente |

---

## 2. Como o fluxo funciona hoje

### Cadastro

```
POST /v1/auth/register { username, email, password }
        │
        ▼
AuthController  →  AuthService.register
                        │  existsByEmail? → 409
                        │  passwordEncoder.encode (BCrypt)
                        ▼
                    User (role = USER fixo) → db_users
```

### Login

```
POST /v1/auth/login { email, password }
        │
        ▼
AuthController → AuthenticationManager
                        │
                        ▼
CustomUserDetailsService.loadUserByUsername(email)
                        │  → UserRepository.findByEmail
                        ▼
BCrypt compara a senha → JwtService.generateToken
                        │  sub = email
                        ▼
                 { "token": "..." }
```

### Requisição autenticada

```
Authorization: Bearer <token>
        │
        ▼
JwtAuthenticationFilter
  ├─ resolveToken: exige prefixo "Bearer "
  ├─ JwtService.extractEmail: valida assinatura e expiração
  ├─ UserRepository.findByEmail
  └─ SecurityContextHolder.setAuthentication
        │
        ▼
SecurityConfig (authorizeHttpRequests) → controller
```

---

## 3. Status geral das correções

### Segurança crítica (P0)

| Item | Status | O que é |
|---|---|---|
| A1 | ✅ Feito | Registro público nunca aceita `role` do cliente. |
| A2 | ✅ Feito | Segredo do JWT obrigatório, sem default. |
| A3 | ✅ Resolvido por decisão | Unicidade passou a ser do **email**; username é descritivo. |
| A4 | ✅ Feito | Constraints nos DTOs e senha com `min 8 / max 72`. |
| A5 | ⏳ Pendente | Rate limiting / lockout no login. |

### Status HTTP e robustez (P1)

| Item | Status | O que é |
|---|---|---|
| B1 | ✅ Feito | Token inválido lança `TokenInvalidoException`, não `RuntimeException`. |
| B2 | ✅ Feito | 401/403 em JSON via `AuthenticationEntryPoint`/`AccessDeniedHandler`. |
| B3 | ✅ Feito | Matchers por método em `/v1/**` (GET `USER`/`ADMIN`; escrita `ADMIN`). |
| B4 | ✅ Feito | `UsernameNotFoundException` quando o email não existe. |
| B5 | ✅ Feito | Header só é aceito com o esquema `Bearer`. |
| B6 | ✅ Feito | Filtro não estoura 500/NPE; segue como anônimo. |
| B7 | ✅ Feito | `findByEmail` retorna `Optional<User>` e o filtro usa `ifPresent`. |
| B8 | ✅ Feito | Registro duplicado responde 409 com `ApiError`. |
| B9 | ✅ Feito | Credenciais inválidas respondem 401. |

### Boas práticas (P2)

| Item | Status | O que é |
|---|---|---|
| C1 | ✅ Feito | `JwtService` usa o `Clock` do projeto. |
| C2 | ✅ Feito | `TokenProperties` com `@ConfigurationProperties` e validação. |
| C3 | ✅ Feito | `issuer` e `expiration` configuráveis, sem duplicação. |
| C4 | ✅ Feito | `UserDetailsAdapter` separa `UserDetails` da entidade `User`. |
| C5 | ✅ Feito | Email usado, validado, `NOT NULL` e `UNIQUE`. |
| C6 | ⏳ Pendente | Refresh token / logout / revogação. |
| C7 | ⏳ Pendente | (Opcional) roles como claim do JWT. |
| C8 | ⏳ Pendente | CORS (quando houver front-end). |
| C9 | ✅ Feito | Testes de auth (service, controller, JWT e filtro). |
| C10 | ✅ Feito | `/auth` está em `/v1/auth` com versionamento nativo do Spring. |
| C11 | ✅ Feito | `JwtService` e `TokenProperties` movidos para `security/jwt`. |
| C12 | ✅ Feito | `authenticationManagerBean` renomeado para `authenticationManager`. |

---

## 4. Decisões de projeto (e o porquê)

### 4.1 Email é a identidade; username é descritivo

**Decisão:** o login é por **email + senha**; o email é único e é o `sub` do JWT. O username continua obrigatório no cadastro, mas **pode repetir**.

**Por quê:** username duplicado é um problema para autenticação — se duas pessoas se chamam "joao", qual conta o login deve abrir? O email resolve isso por ser naturalmente único. Para permitir a mudança, o `subject` do token também passou a ser o email.

**Consequência:** os migrations `V14` (adicionou `UNIQUE` em username) e `V15` (removeu) parecem se contradizer, mas contam a história real: primeiro tentamos username único, depois decidimos que a identidade seria o email. Nunca editamos `V14` porque ela já havia sido aplicada — o Flyway valida o checksum. A forma correta de desfazer é uma migration nova (`V15`).

### 4.2 `AuthService` existe para tirar regra do controller

**Decisão:** o cadastro saiu do `AuthController` e foi para `AuthService`; o login continua no controller.

**Por quê:** todos os outros domínios do projeto seguem `entity → repository → service → controller`. O auth era a exceção. Com o service:
- a regra fica testável com Mockito puro;
- o controller só roteia;
- o erro de duplicidade vira exceção de domínio, tratada de forma central.

### 4.3 Senha: BCrypt e o limite de 72 bytes

**Decisão:** política **mínimo 8 / máximo 72 caracteres** (aplicação no DTO feita).

**Por quê:** o BCrypt ignora bytes além de 72. Aceitar uma senha de 200 caracteres dá uma falsa sensação de segurança: os últimos 128 não contam. O mínimo 8 evita senhas triviais. O hash gerado tem ~60 caracteres, por isso a coluna do banco é `varchar(255)`.

### 4.4 Segredo do JWT obrigatório (*fail fast*)

**Decisão:** remover o default `my_secret_key`; a aplicação **não sobe** sem `API_SECURITY_TOKEN_SECRET`.

**Por quê:** o segredo é a chave que assina e valida tokens. Um default público permite que qualquer pessoa forge um token de admin. É melhor o sistema falhar rápido no boot do que subir silenciosamente inseguro.

### 4.5 Token inválido é "anônimo", não erro 500

**Decisão:** o `JwtAuthenticationFilter` captura `TokenInvalidoException`, limpa o contexto e **continua** a requisição.

**Por quê:** um token ruim é erro do cliente. O filtro roda **antes** do controller; por isso o `@RestControllerAdvice` não o alcança. Deixar a requisição seguir sem autenticação faz o Spring Security aplicar a regra da rota (401/403) de forma natural.

---

## 5. O que já foi feito (com explicação)

### 5.1 A1 — Papel fixo no registro

**Problema:** o `RegisterRequest` tinha `role`; o cliente podia mandar `"ADMIN"` e virar administrador.

**Correção:** o campo foi removido e o `AuthService` grava sempre `UserRole.USER`.

**Onde:** `RegisterRequest.java`, `AuthService.java`.

**Teste:** `AuthControllerTest` garante que o registro delega ao service; o service fixa a role.

---

### 5.2 A2 — Segredo obrigatório

**Problema:** `${API_SECURITY_TOKEN_SECRET:my_secret_key}` — sem a variável, o app usava uma chave pública.

**Correção:**

```yaml
# application.yaml
api:
  security:
    token:
      secret: ${API_SECURITY_TOKEN_SECRET}
```

- `.env.example` ganhou `API_SECURITY_TOKEN_SECRET=`.
- O `.env` local recebeu um segredo gerado com `secrets.token_urlsafe(48)`.
- `application-test.yaml` tem um valor fixo, para os testes não dependerem do `.env`/CI.

**Verificação:** sem `.env` e sem a variável, o boot falha com `Could not resolve placeholder 'API_SECURITY_TOKEN_SECRET'`.

---

### 5.3 Identidade por email (V12–V15)

| Migration | O que faz |
|---|---|
| `V12` | `email` passa a ser `NOT NULL`. |
| `V13` | Cria `uk_db_users_email` (email único). |
| `V14` | Cria `uk_db_users_username` (tentativa inicial). |
| `V15` | Remove `uk_db_users_username` (username volta a poder repetir). |

**Conceito:** `NOT NULL` garante que o valor existe; `UNIQUE` garante que não se repete. A validação no service dá a mensagem amigável, mas a constraint no banco é a garantia final — inclusive contra requisições concorrentes (TOCTOU: "checa, depois age").

---

### 5.4 A4 — Bean Validation nos DTOs (parcial)

**Problema:** o controller usava `@Valid`, mas os records não tinham nenhuma anotação — validar nada.

**Correção atual:**

```java
public record RegisterRequest(
        @NotBlank @Size(max = 20) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {}
```

```java
public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank String password
) {}
```

**Aplicado:** a senha agora usa `@Size(min = 8, max = 72)`; o login continua só com `@NotBlank`.

**Conceito:** `@NotBlank` rejeita nulo/vazio/espaços; `@Email` valida formato; `@Size` limita tamanho. Quando falham, o `GlobalExceptionHandler` devolve 400 com a lista de campos.

---

### 5.5 B1 — `TokenInvalidoException`

**Problema:** `extractEmail` lançava `RuntimeException("Error while validating token")`, que ninguém tratava → 500.

**Correção:**

```java
} catch (JWTVerificationException exception) {
    throw new TokenInvalidoException("Token inválido ou expirado");
}
```

**Conceito:** exceção de domínio tem nome e significado; `RuntimeException` genérica não diz o que fazer. Quem chama consegue distinguir "token ruim" de "bug no servidor".

---

### 5.6 B3 — Matchers de autorização

**Problema:** `.requestMatchers("/v1/atletas").hasRole("ADMIN")` casava só o caminho exato; `/v1/atletas/1` caía na regra de `USER`.

**Correção atual** (`SecurityConfig`): regras por método HTTP em `/v1/**`:

- `GET /v1/**` → `USER` ou `ADMIN`;
- `POST`, `PATCH`, `PUT`, `DELETE /v1/**` → `ADMIN`;
- `/v1/auth/**` → público;
- restante → `USER`.

**Conceito:** a ordem importa. Regras mais específicas vêm antes do `anyRequest()`, que é o fallback.

---

### 5.7 B5 — Prefixo `Bearer`

**Problema:** `authHeader.replace("Bearer ", "")` removia o texto em qualquer posição e aceitava qualquer esquema.

**Correção:**

```java
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    return null;
}
return authHeader.substring(7);
```

**Conceito:** o header `Authorization` tem **esquema + credencial**. Validar o esquema evita tratar `Token abc` como se fosse um JWT.

---

### 5.8 B6 — Filtro robusto

**Problema:** token inválido → exceção no filtro → 500; usuário deletado → `findByEmail` nulo → NPE → 500.

**Correção:**

```java
try {
    var email = jwtService.extractEmail(token);
    var userDetails = userRepository.findByEmail(email);
    if (userDetails != null) {
        // autentica
    }
} catch (TokenInvalidoException e) {
    SecurityContextHolder.clearContext();
}
```

**Conceito:** o filtro não decide o status HTTP; ele decide **se há autenticação**. Quem decide 401/403 é o Spring Security. Isso também explica por que o `@RestControllerAdvice` não resolve: ele só atua depois do DispatcherServlet.

---

### 5.9 B8 — Registro duplicado com 409

**Problema:** o controller respondia `400` com corpo vazio para username duplicado.

**Correção:** o `AuthService` pré-checa `existsByEmail` e lança `EmailJaCadastradoException`; o `GlobalExceptionHandler` devolve **409** com `ApiError`. Se a corrida escapar do pré-check, o `DataIntegrityViolationException` cujo texto contém `uk_db_users_email` é traduzido para a mesma exceção.

---

### 5.10 B9 — Credenciais inválidas com 401

**Problema:** senha errada lançava `BadCredentialsException` e virava 500.

**Correção:** handler de `AuthenticationException` (classe-mãe de `BadCredentialsException`) devolve **401** com `"Credenciais inválidas"`.

**Conceito:** a mensagem é genérica de propósito — não revela se o email existe (enumeração de usuários).

---

### 5.11 C5 — Email deixou de ser campo morto

Antes o `email` existia no banco, mas o registro não o preenchia. Agora ele é preenchido, validado, `NOT NULL`, `UNIQUE` e usado como identidade de login.

---

### 5.12 C9 — Testes de autenticação

| Teste | O que cobre |
|---|---|
| `AuthServiceTest` | sucesso, email duplicado, username duplicado permitido, violação de integridade. |
| `AuthControllerTest` | login por email, 401, 400 de validação, register e 409. |
| `JwtServiceTest` | token válido, assinatura errada, expirado, malformado. |
| `JwtAuthenticationFilterTest` | token válido autentica; inválido/sem Bearer/usuário ausente seguem anônimos. |

Total atual: **251 testes**.

### 5.13 B7 — `findByEmail` com `Optional`

**Problema:** o repositório devolvia `UserDetails` ou `null`; o `null` não aparecia no tipo e o filtro precisava lembrar de checar.

**Correção:**

```java
Optional<User> findByEmail(String email);
```

- `CustomUserDetailsService` usa `.orElseThrow(...)`;
- `JwtAuthenticationFilter` usa `.ifPresent(...)`.

**Conceito:** `Optional` obriga quem chama a decidir o que fazer quando o valor não existe; o `if (userDetails != null)` deixa de ser necessário.

### 5.14 B4 — `UsernameNotFoundException`

**Problema:** o contrato do `UserDetailsService` manda lançar `UsernameNotFoundException` quando o usuário não existe; devolver `null` virava `NullPointerException`/500.

**Correção:**

```java
return userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
```

**Detalhe:** o `DaoAuthenticationProvider` converte essa exceção em `BadCredentialsException` por padrão, então o login continua respondendo 401 genérico — sem revelar se o email existe.

### 5.15 B2 — 401/403 em JSON

**Problema:** o Spring Security respondia no formato padrão dele, fora do contrato `ApiError`.

**Correção:** `RestAuthenticationEntryPoint` (401) e `RestAccessDeniedHandler` (403), ambos usando `ApiErrorResponseWriter` com o `ObjectMapper` do Jackson 3 (`tools.jackson.databind.ObjectMapper`). O `SecurityConfig` registra os dois em `exceptionHandling(...)`.

**Conceito:** o `@RestControllerAdvice` não captura exceções de filtros. O `ExceptionTranslationFilter` decide entre entry point (anônimo → 401) e access denied handler (autenticado sem role → 403).

**Verificação:** sem token → `401 {"message":"Não autenticado",...}`; token de `USER` em `POST /v1/**` → `403 {"message":"Acesso negado",...}`. Os testes dos handlers ficaram para a iteração de testes.

### 5.16 C1/C2/C3 — `Clock` e configuração do token

**Problema:** o `JwtService` usava `LocalDateTime.now()` com offset fixo `-03:00`, `@Value` sem validação e issuer/expiração repetidos/no código.

**Correção:**
- `TokenProperties` (`@ConfigurationProperties(prefix = "api.security.token")`, `@Validated`) com `secret` (`@NotBlank @Size(min = 32)`), `issuer` e `expiration` (`Duration`).
- `JwtService` recebe `TokenProperties` e `Clock` por construtor; `expirationDate()` = `clock.instant().plus(properties.expiration())`.
- `application.yaml` e `application-test.yaml` definem `issuer` e `expiration: 2h`.

**Conceitos:** `Clock` abstrai o “agora” (permite `Clock.fixed` nos testes); `@ConfigurationProperties` agrupa e valida a configuração; `Duration` converte `2h` automaticamente.

**Verificação:** token emitido com `iss=gestao-esportiva-api` e `exp` exatamente 2h à frente; acesso continua 200.

### 5.17 C4 — `UserDetailsAdapter`

**Problema:** `User implements UserDetails` acoplava a entidade JPA ao Spring Security e herdava os setters de `password`/`role` na camada de segurança.

**Correção:**
- `User` voltou a ser uma entidade pura (sem imports de `org.springframework.security`).
- `UserDetailsAdapter` (record em `security/users`) embrulha `User` e implementa `UserDetails`: `getAuthorities`, `getPassword`, `getUsername` (agora devolve o **email**), `isEnabled`, etc.
- `CustomUserDetailsService` e `JwtAuthenticationFilter` passaram a usar `.map(UserDetailsAdapter::new)`.
- `AuthController` obtém a entidade via `userDetails.user()` para gerar o token.

**Conceito:** adapter traduz um objeto de um mundo (domínio/persistência) para a interface que outro mundo (segurança) entende. A entidade deixa de depender do framework e os setters perigosos saem da superfície de autenticação.

**Efeito colateral positivo:** `Authentication.getName()` passa a devolver o **email** (antes devolvia `username`), alinhado à decisão de identidade por email.

**Teste:** `UserDetailsAdapterTest` cobre email como username, delegação da senha, authorities de `USER`/`ADMIN` e flags de conta. Total: **251 testes**.

### 5.18 C10 — Versionamento nativo do Spring (`/v1/auth`)

**Problema:** `/auth/**` era o único recurso fora de `/v1`; o versionamento era só convenção de path, sem validação.

**Correção:**
- `config/ApiVersioningConfig` (`WebMvcConfigurer.configureApiVersioning`) usa `usePathSegment(0, predicate)`, suporta `1.0` e define `1.0` como default. O predicate só considera versionados paths `/vN/...`, então `/error` continua fora.
- Todos os controllers passaram de `@RequestMapping("/v1/recurso")` para `@RequestMapping(path = "/v{version}/recurso", version = "1.0")` — inclusive `AuthController` (`/v1/auth`).
- `SecurityConfig` libera `/v1/auth/**`; as demais regras `/v1/**` seguem iguais.
- Versão não suportada (ex.: `/v9/...`) → **400** (`InvalidApiVersionException`), antes de chegar ao controller.

**Conceito:** o Spring 7 tem três peças — resolver (de onde vem a versão), parser (`SemanticApiVersionParser`, aceita `v1`) e mapping (`version` no `@RequestMapping`). Antes disso, versionar era só nome de URL sem garantia.

**Teste:** `AuthControllerTest` cobre `/v1/auth/*` e rejeição de `/v9/auth/login`; `ApiVersioningConfigTest` cobre o predicate de paths versionados. Total: **251 testes**.

### 5.19 C11 — `JwtService` e `TokenProperties` em `security/jwt`

**Problema:** `JwtService` é um serviço (comportamento), mas morava em `security/config` junto de `SecurityConfig` (wiring). `TokenProperties` só existe para o JWT e também estava em `config`.

**Correção:** criado o pacote `security/jwt`, que agora concentra `JwtService` e `TokenProperties`. `security/config` ficou com `SecurityConfig`, `JwtAuthenticationFilter` e os handlers `Rest*`. Imports atualizados em `AuthController`, `JwtAuthenticationFilter`, `SecurityConfig` e nos testes.

**Conceito:** pacote deve refletir responsabilidade. `config` = montagem/wiring; `jwt` = tudo relacionado a token (serviço + propriedades). Isso prepara o terreno para refresh token (C6).

### 5.20 C12 — `authenticationManagerBean` → `authenticationManager`

**Problema:** o método `@Bean` tinha o sufixo redundante `Bean`; o nome do método é o nome do bean e a injeção no projeto é por tipo.

**Correção:** `SecurityConfig.authenticationManagerBean(...)` renomeado para `authenticationManager(...)`. Nenhum `@Qualifier`/lookup por nome usava o nome antigo, então a mudança é segura.

**Conceito:** em `@Bean`, o nome do método vira o nome do bean. O sufixo `Bean` é estilo antigo; o padrão atual é usar o nome do tipo.

---

## 6. O que ainda falta (com o porquê)

### 6.1 A5 — Rate limiting / lockout

Hoje dá para tentar senhas infinitamente. Opções: bucket4j/Resilience4j (limite por IP/usuário) ou campos de tentativas/bloqueio no `User` + `isAccountNonLocked()`. É a defesa contra força bruta.

### 6.2 C6 — Refresh token / logout

O token dura 2h e não há revogação. Se for preciso invalidar antes do vencimento (troca de senha, logout), JWT puro não basta: precisa de refresh token + blacklist/allowlist.

### 6.3 C7/C8 — Roles no token e CORS

- **C7:** colocar roles no JWT evita um SELECT por request, mas perde revogação imediata. Trade-off performance × revogação.
- **C8:** configurar origens permitidas quando existir front-end.

---

## 7. Fora da segurança (roadmap do projeto)

Itens já mapeados em `futuras-iterações.md`:

- **`hardDelete` de atleta** — desbloqueado, pois o perfil `ADMIN` existe; falta endpoint + regra de FKs.
- **`GET /v1/atletas/cpf/{cpf}`** — ✅ feito.
- **OpenAPI/springdoc + README** — documentar a API e a estratégia de versionamento.
- **Separar `application-dev`/`application-prod`** — `show-sql` ligado globalmente hoje.
- **`AtletaControllerTest` + `AtletaMapperTest`** — lacunas de teste no domínio.
- **CI (GitHub Actions)** — rodar `./mvnw test` a cada PR.
- **Testcontainers** — opcional, para testes de integração sem MySQL local.

---

## 8. Conceitos que apareceram (resumo de estudo)

- **JWT**: string assinada. O `sub` (subject) identifica o dono; `exp` define validade; a assinatura impede adulteração.
- **Secret**: chave HMAC usada para assinar/validar. Se vaza, qualquer um forja tokens.
- **BCrypt**: hash lento e com salt; não é criptografia reversível. Ignora bytes além de 72.
- **Bearer**: esquema do header `Authorization`; o token vai depois de `Bearer `.
- **Filtro × ControllerAdvice**: filtros rodam antes do DispatcherServlet; por isso exceções de filtro não são capturadas por `@ExceptionHandler`.
- **TOCTOU / race condition**: "checa, depois age" pode falhar com concorrência. A constraint `UNIQUE` no banco é a garantia final.
- **401 × 403**: 401 = não autenticado (quem é você?); 403 = autenticado, mas sem permissão.
- **Fail fast**: falhar no boot é melhor que subir com configuração insegura.
- **`Optional`**: expressa "pode não existir" no tipo, em vez de devolver `null`.

---

## 9. Como verificar

```bash
# suíte completa (H2, sem MySQL)
./mvnw test

# subir a aplicação (precisa de API_SECURITY_TOKEN_SECRET no .env)
./mvnw spring-boot:run

# login
curl -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@quixada.gov.br","password":"admin@123"}'

# acesso com token
curl http://localhost:8080/v1/atletas -H "Authorization: Bearer <token>"

# token inválido -> 401 JSON
curl http://localhost:8080/v1/atletas -H "Authorization: Bearer lixo"
```

**Estado atual:** 251 testes, 0 falhas; boot falha sem o segredo; sem token/token inválido → 401 JSON; role insuficiente → 403 JSON.
