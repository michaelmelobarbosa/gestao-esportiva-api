# Security Fixes — Checklist de Correções (Autenticação & Autorização)

> Pasta `src/main/java/br/gov/quixada/esporte/iterations` — documento de estudo e roadmap das correções de segurança.
> Base da análise: estado do código após a reorganização dos pacotes em `security/config`, `security/auth` e `security/users`.

A autenticação **funciona no fluxo feliz**: `POST /auth/login` emite um JWT, o `JwtAuthenticationFilter` valida o token a cada request e as senhas são guardadas com BCrypt. O problema é que a implementação está no estilo "tutorial": faltam validações, constraints no banco e tratamento de erro adequado. Vários desses buracos são exploráveis.

Este documento lista as correções **na ordem de prioridade** (mais críticas primeiro), com explicação do *porquê*, *onde* e *como* corrigir. Cada item é independente o suficiente para ser implementado sozinho.

---

## Como ler este documento

- **P0** = crítico / explorável → corrigir primeiro.
- **P1** = importante → comportamento errado, mas menos grave.
- **P2** = melhoria / boa prática → pode ficar para depois.
- Cada item tem: `Onde` (arquivo), `Por quê`, `Como corrigir` (com exemplo) e `Como testar`.

### Conceitos rápidos que aparecem aqui

- **Escalonamento de privilégio (privilege escalation):** quando um usuário comum consegue virar administrador sozinho. Acontece quando o servidor aceita do cliente algo que só ele deveria decidir (ex.: o papel/role).
- **Race condition:** duas requisições simultâneas passam pela mesma checagem antes de qualquer uma gravar, e o "estado ruim" acaba sendo gravado duas vezes. Um `UNIQUE` no banco é a única forma 100% segura de evitar isso.
- **Constraint de banco vs. validação em código:** a validação em código dá boa mensagem de erro; a constraint no banco é a garantia final. Precisa das duas.
- **JWT (JSON Web Token):** string assinada com um segredo. Quem tem o segredo pode forjar tokens. O `sub` (subject) é o "dono" do token; normalmente guardamos o username ali.
- **Stateless:** o servidor não guarda sessão; cada request prova quem é pelo token. Por isso CSRF pode ficar desabilitado.

---

## 🔴 Categoria A — Segurança crítica (P0)

### A1. Impedir escalonamento de privilégio no registro

- [ ] Fixar o papel do registro público como `USER` (ignorar o que vier do cliente).

**Onde:** `security/auth/AuthController.java` (`register`) e `security/users/dto/RegisterRequest.java`.

**Por quê:** hoje o `RegisterRequest` tem o campo `role` e o controller grava exatamente o que o cliente mandar (`new User(request.username(), encryptedPassword, request.role())`). Basta:

```json
POST /auth/register
{ "username": "hacker", "password": "123", "role": "ADMIN" }
```

para qualquer pessoa criar um administrador — e `ADMIN` tem acesso a endpoints restritos (ex.: `/v1/atletas`). Isso é uma falha grave.

**Como corrigir (passo a passo):**

1. Remover o campo `role` do `RegisterRequest`:
   ```java
   public record RegisterRequest(
           @NotBlank String username,
           @NotBlank @Size(min = 8, max = 72) String password
   ) {}
   ```
2. No `register`, sempre usar `UserRole.USER`:
   ```java
   var user = new User(request.username(), encryptedPassword, UserRole.USER);
   ```
3. Se um dia precisar criar `ADMIN`, criar um endpoint separado e protegido:
   ```java
   @PreAuthorize("hasRole('ADMIN')") // exige token de admin
   @PostMapping("/users")
   ...
   ```
   ou criar o primeiro admin via migration/seed.

**Como testar:** `POST /auth/register` com `"role":"ADMIN"` no corpo → o usuário criado deve ser `USER`. Faça login com ele e tente acessar um endpoint de admin → deve receber **403**.

---

### A2. Remover o valor padrão (`default`) do segredo do JWT

- [ ] Não permitir que a aplicação suba com um segredo previsível.

**Onde:** `src/main/resources/application.yaml:29`.

**Por quê:** hoje está assim:

```yaml
api:
  security:
    token:
      secret: ${API_SECURITY_TOKEN_SECRET:my_secret_key}
```

O trecho `:my_secret_key` é o valor usado **quando a variável de ambiente não existe**. Ou seja, se ninguém configurar `API_SECURITY_TOKEN_SECRET`, a aplicação sobe com um segredo público e qualquer um que conheça esse valor consegue **forjar tokens válidos** (logar como qualquer usuário, inclusive admin). Além disso, `my_secret_key` é curto (~104 bits); o recomendado para HMAC-SHA256 é ≥ 256 bits.

**Como corrigir:**

1. Remover o default para a app **falhar no boot** se a variável não existir:
   ```yaml
   secret: ${API_SECURITY_TOKEN_SECRET}
   ```
2. Adicionar `API_SECURITY_TOKEN_SECRET=` no `.env.example` e documentar no `.env` real.
3. Gerar um segredo forte, por exemplo:
   ```bash
   openssl rand -base64 48
   ```
4. Em produção, esse valor **nunca** vai para o Git (o `.env` já é gitignored).

**Como testar:** subir a aplicação sem a variável definida → deve falhar com erro claro de propriedade ausente. Subir com a variável → login funciona normalmente.

---

### A3. Adicionar `UNIQUE` em `username` no banco

- [ ] Criar migration `V10` com `unique` em `username`.
- [ ] Tratar a violação no código com mensagem amigável.

**Onde:** `src/main/resources/db/migration/` (nova migration) e `security/auth/AuthController.java` (`register`).

**Por quê:** a tabela `db_users` não tem `UNIQUE` em `username`. O `register` faz "checa se existe, depois insere" — entre a checagem e o insert, duas requisições simultâneas podem passar e criar **dois usuários com o mesmo username**. Depois, o `findByUsername` retornaria 2 resultados e o login quebraria (`IncorrectResultSizeDataAccessException`).

**Como corrigir:**

1. Nova migration (nunca edite migrations já aplicadas — o Flyway valida o checksum):
   ```sql
   -- V10__unique_username.sql
   alter table db_users
       add constraint uk_db_users_username unique (username);
   ```
   > Se já existirem usernames duplicados no banco, limpe-os antes.
2. Tratar o erro no service/controller para devolver uma mensagem clara (409 Conflict), reaproveitando o padrão do projeto:
   ```java
   try {
       userRepository.save(user);
   } catch (DataIntegrityViolationException e) {
       throw new UsuarioJaCadastradoException("Username já cadastrado");
   }
   ```
   O `GlobalExceptionHandler` já sabe transformar exceções de domínio em `ApiError`.

**Como testar:** dois `POST /auth/register` com o mesmo username → o segundo deve retornar **409** com `ApiError`, não 500.

---

### A4. Validar a entrada (Bean Validation nos DTOs)

- [ ] Adicionar constraints em `LoginRequest` e `RegisterRequest`.

**Onde:** `security/users/dto/LoginRequest.java` e `RegisterRequest.java`.

**Por quê:** o controller usa `@Valid`, mas os records **não têm nenhuma constraint**. Então `@Valid` não valida nada: dá para registrar username nulo/vazio e senha vazia. Sem isso, o banco estoura `not null` e o cliente recebe 500 em vez de 400 com a lista de erros.

**Como corrigir:**

```java
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
```

```java
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 20) String username,
        @NotBlank @Size(min = 8, max = 72) String password
) {}
```

Observações:
- `max = 72` na senha porque o **BCrypt ignora bytes além de 72** — limitar evita confusão.
- O `GlobalExceptionHandler` já trata `MethodArgumentNotValidException` e devolve 400 com `ApiError`. Não precisa mexer nele.

**Como testar:** `POST /auth/register` com `password` de 2 caracteres → **400** com a lista de campos inválidos.

---

### A5. Proteção contra força bruta (brute force)

- [ ] Adicionar rate limiting no endpoint de login.
- [ ] (Opcional) Lockout progressivo após N tentativas.

**Onde:** `security/auth/AuthController.java` (`/auth/login`) e `security/users/User.java` (`isAccountNonLocked`).

**Por quê:** hoje nada impede tentar senhas infinitamente (`isAccountNonLocked()` retorna sempre `true`). Um atacante pode fazer força bruta no login.

**Como corrigir (opções, da mais simples para a mais completa):**

1. **Rate limit por IP/usuário** com `bucket4j` ou `Resilience4j` — limita N tentativas por minuto.
2. **Lockout** usando a própria estrutura do Spring Security: adicionar campos no `User` (ex.: `tentativasFalhas`, `bloqueadoAte`) e fazer `isAccountNonLocked()` refletir isso.
3. Em infraestrutura, um WAF/proxy (nginx, API Gateway) também resolve.

**Como testar:** disparar várias requisições de login com senha errada → a partir da N-ésima deve retornar 429 (rate limit) ou 401 com conta bloqueada.

---

## 🟠 Categoria B — Status HTTP correto e robustez (P1)

### B1. Token inválido/expirado deve virar 401 (e não 500)

- [ ] Substituir a `RuntimeException` genérica por uma exceção específica.
- [ ] Mapear essa exceção para 401 no `GlobalExceptionHandler` (ou no filtro).

**Onde:** `security/config/JwtService.java` (métodos `generateToken`/`extractUsername`) e `common/error/GlobalExceptionHandler.java`.

**Por quê:** `extractUsername` captura `JWTVerificationException` e lança `RuntimeException("Error while validating token")`. Como o `GlobalExceptionHandler` não conhece essa exceção, o resultado é **500**. Um token inválido é um erro do cliente → deve ser **401**.

**Como corrigir:**

1. Criar exceção de domínio, por exemplo `TokenInvalidoException`.
2. Lançá-la no `JwtService` no lugar da `RuntimeException`.
3. Registrar no `GlobalExceptionHandler`:
   ```java
   @ExceptionHandler(TokenInvalidoException.class)
   public ResponseEntity<ApiError> handleTokenInvalido(TokenInvalidoException ex, HttpServletRequest request) {
       return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
   }
   ```

**Como testar:** chamar um endpoint protegido com `Authorization: Bearer lixo` → **401** com `ApiError`.

---

### B2. Respostas JSON para 401 e 403 (EntryPoint / AccessDeniedHandler)

- [ ] Configurar `AuthenticationEntryPoint` (401) e `AccessDeniedHandler` (403).

**Onde:** `security/config/SecurityConfig.java`.

**Por quê:** quando o Spring Security bloqueia por falta de token (401) ou por falta de permissão (403), ele responde no formato padrão dele (vazio/HTML), **inconsistente** com o `ApiError` do resto da API. Um front-end que espera JSON quebra.

**Como corrigir:** registrar handlers que escrevem `ApiError`:

```java
.exceptionHandling(ex -> ex
        .authenticationEntryPoint((req, res, e) -> writeApiError(res, 401, "Não autenticado"))
        .accessDeniedHandler((req, res, e) -> writeApiError(res, 403, "Acesso negado")))
```

(ou extrair para beans/classes próprias para ficar mais limpo).

**Como testar:** acessar endpoint protegido sem token → 401 JSON; com token de USER em endpoint de ADMIN → 403 JSON.

---

### B3. Corrigir o matcher de ADMIN para cobrir o recurso inteiro

- [ ] Trocar `/v1/atletas` por `/v1/atletas` + `/v1/atletas/**`.

**Onde:** `security/config/SecurityConfig.java:32`.

**Por quê:** `.requestMatchers("/v1/atletas").hasRole("ADMIN")` casa **só exatamente** `/v1/atletas`. As rotas `/v1/atletas/{id}` (buscar/editar/alterar status) caem em `.anyRequest().hasRole("USER")` → **qualquer usuário comum acessa**. Provavelmente não era a intenção.

**Como corrigir:**

```java
.requestMatchers("/v1/atletas", "/v1/atletas/**").hasRole("ADMIN")
```

> Alternativa idiomática: usar `@PreAuthorize("hasRole('ADMIN')")` direto no controller (mais perto da regra, mais fácil de ler).

**Como testar:** login como USER e chamar `GET /v1/atletas/1` → deve dar **403**.

---

### B4. `CustomUserDetailsService` deve lançar `UsernameNotFoundException`

- [ ] Lançar a exceção quando o usuário não existir.

**Onde:** `security/auth/CustomUserDetailsService.java:20`.

**Por quê:** o contrato da interface `UserDetailsService` diz que, se o usuário não for encontrado, devemos lançar `UsernameNotFoundException`. Hoje o método retorna o que o repositório devolver — possivelmente `null` — e isso pode virar `NullPointerException`/`InternalAuthenticationServiceException`. Repare que o `throws UsernameNotFoundException` já está declarado, mas nunca é usado.

**Como corrigir:**

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
}
```

**Como testar:** tentar login com username inexistente → 401 (ou 403, conforme a config), sem stack trace de NPE nos logs.

---

### B5. Validar o prefixo `Bearer` ao extrair o token

- [ ] Trocar o `replace` por uma checagem de prefixo.

**Onde:** `security/config/JwtAuthenticationFilter.java:37-42`.

**Por quê:** `authHeader.replace("Bearer ", "")` remove o texto onde ele aparecer, sem validar. Um header `Authorization: qualquercoisa` vira token `qualquercoisa` e é processado. O correto é exigir o esquema `Bearer`.

**Como corrigir:**

```java
private String resolveToken(HttpServletRequest request) {
    var authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return null;
    }
    return authHeader.substring(7);
}
```

**Como testar:** enviar `Authorization: Token abc` → tratado como sem token (401 no endpoint protegido), não como token inválido.

---

### B6. Evitar NPE quando o usuário do token não existe mais

- [ ] Checar `null`/`Optional` antes de usar o `UserDetails`.

**Onde:** `security/config/JwtAuthenticationFilter.java:28-31`.

**Por quê:** se o token é válido mas o usuário foi deletado do banco, `findByUsername` devolve `null` e o `userDetails.getAuthorities()` da linha seguinte estoura NPE → 500. Além disso, se `extractUsername` lançar (token inválido), a exceção sobe e a request vira 500.

**Como corrigir:** deixar o filtro "falhar silenciosamente" para token inválido/usuário ausente (a request segue como anônima e o Spring devolve 401/403 conforme a rota):

```java
try {
    var username = jwtService.extractUsername(token);
    userRepository.findByUsername(username).ifPresent(user -> {
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    });
} catch (TokenInvalidoException e) {
    SecurityContextHolder.clearContext(); // segue sem autenticação
}
filterChain.doFilter(request, response);
```

**Como testar:** gerar token válido, apagar o usuário do banco, chamar endpoint protegido → 401 (não 500).

---

### B7. `findByUsername` deve retornar `Optional<User>`

- [ ] Trocar o tipo de retorno e ajustar os usos.

**Onde:** `security/auth/UserRepository.java:9`.

**Por quê:** hoje retorna `UserDetails` (a interface do Spring Security), o que não é idiomático para um repositório Spring Data e obriga o controller a comparar com `!= null`. O padrão é retornar `Optional<User>`.

**Como corrigir:**

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

Ajustar os 3 usos: `AuthController` (`isPresent()`), `CustomUserDetailsService` (`orElseThrow`) e `JwtAuthenticationFilter` (`ifPresent`).

**Como testar:** compilar e rodar os testes existentes (`./mvnw clean test`); o contexto deve subir normalmente.

---

### B8. Padronizar o erro do `register` com `ApiError`

- [ ] Parar de responder `badRequest()` sem corpo.

**Onde:** `security/auth/AuthController.java:42-44`.

**Por quê:** o resto da API sempre responde com `ApiError` (status, mensagem, timestamp, path). O `register` devolve 400 **sem corpo**, o que quebra o contrato. Melhor ainda: mover essa regra para um `AuthService` e lançar uma exceção de domínio (ex.: `UsuarioJaCadastradoException`), deixando o `GlobalExceptionHandler` montar o `ApiError`.

**Como corrigir:**

```java
if (userRepository.findByUsername(request.username()).isPresent()) {
    throw new UsuarioJaCadastradoException("Username já cadastrado");
}
```

**Como testar:** registrar username repetido → 409 com `ApiError` no corpo.

---

### B9. Tratar `BadCredentialsException` no login

- [ ] Mapear credenciais inválidas para 401 com `ApiError`.

**Onde:** `security/auth/AuthController.java` (`login`) e/ou `GlobalExceptionHandler`.

**Por quê:** senha errada faz o `authenticationManager.authenticate(...)` lançar `BadCredentialsException`, que não é tratada → 500. O correto é **401**.

**Como corrigir:** adicionar no `GlobalExceptionHandler`:

```java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos", request, List.of());
}
```

> Cuidado: a mensagem não deve dizer se o erro foi no usuário ou na senha (evita enumeração de usuários).

**Como testar:** login com senha errada → 401 com mensagem genérica.

---

## 🟡 Categoria C — Boas práticas e melhorias (P2)

### C1. Usar o `Clock` do projeto no `JwtService`

- [ ] Injetar `Clock` e calcular expiração a partir dele.

**Onde:** `security/config/JwtService.java:53-54`.

**Por quê:** o projeto já tem um bean `Clock` (`config/ClockConfig.java`, fuso `America/Fortaleza`), mas o `JwtService` usa `LocalDateTime.now()` + offset fixo `-03:00`. `Instant` não tem fuso; o offset fixo é frágil (não acompanha mudanças de regra de fuso) e dificulta testes.

**Como corrigir:**

```java
private final Clock clock; // injetado

private Instant expirationDate() {
    return Instant.now(clock).plus(2, ChronoUnit.HOURS);
}
```

**Como testar:** com um `Clock.fixed(...)` nos testes, verificar que o `exp` do token é exatamente `agora + 2h`.

---

### C2. Trocar `@Value` por `@ConfigurationProperties`

- [ ] Criar um record de propriedades validado.

**Onde:** `security/config/JwtService.java:18-19` e `application.yaml`.

**Por quê:** `@Value("${api.security.token.secret}")` não valida nada, não tem tipagem e dificulta testar. `@ConfigurationProperties` é o jeito idiomático e permite `@NotBlank` na propriedade.

```java
@ConfigurationProperties(prefix = "api.security.token")
public record TokenProperties(@NotBlank String secret, Duration expiration) {}
```

**Como testar:** subir sem `secret` → falha de validação no boot.

---

### C3. Tornar expiração e issuer configuráveis / constantes

- [ ] Extrair `"gestao-esportiva-api"` (duplicado) para constante.
- [ ] Mover `plusHours(2)` para propriedade.

**Onde:** `security/config/JwtService.java:27,43,54`.

**Por quê:** o issuer aparece duas vezes (risco de divergir) e a expiração de 2h está "mágica" no código. Configurável via `api.security.token.expiration=2h` facilita mudar sem recompilar.

---

### C4. Repensar `User` (entidade JPA) implementando `UserDetails`

- [ ] Avaliar separar domínio de integração com o Spring Security.

**Onde:** `security/users/User.java`.

**Por quê:** hoje a entidade de banco implementa `UserDetails` e tem `@Setter` + `@AllArgsConstructor`. Isso:
- acopla o modelo de dados ao framework de segurança;
- expõe setters para `password` e `role`, permitindo mutação indevida em qualquer lugar.

Em projetos maiores, cria-se um `UserDetails` separado (adapter) e mantém a entidade "pura". Não é urgente, mas é uma decisão de arquitetura importante.

---

### C5. `email`: campo morto, sem validação e sem uso

- [ ] Decidir o destino do `email` (usar ou remover).
- [ ] Se usar: `@Email`, `unique` e preencher no registro.

**Onde:** `security/users/User.java:26-27`, `RegisterRequest`, migrations.

**Por quê:** o campo existe no banco e na entidade, mas o construtor usado no registro não o preenche — nunca é gravado. Além disso não tem `@Email` nem `unique`. Ou passa a ser usado (ex.: login por email, recuperação de senha), ou é removido para não confundir.

---

### C6. Refresh token / logout / revogação

- [ ] Decidir a estratégia de sessão de longo prazo.

**Onde:** fluxo de auth como um todo.

**Por quê:** o token dura 2h e não há refresh nem logout no servidor. Ao expirar, o usuário precisa logar de novo. Se for necessário revogar um token antes de expirar (ex.: troca de senha), JWT puro não resolve — precisa de refresh token + blacklist/allowlist.

---

### C7. (Opcional) Incluir roles no token

- [ ] Avaliar colocar as authorities como claim do JWT.

**Onde:** `JwtService` + `JwtAuthenticationFilter`.

**Por quê:** hoje o filtro faz **um SELECT no banco por request** para carregar o usuário e suas authorities. Colocar as roles no token evita essa consulta, mas perde a revogação imediata (se mudar a role, o token antigo continua com a role velha até expirar). É um trade-off: desempenho × revogação.

---

### C8. Configurar CORS (se houver front-end)

- [ ] Definir origens permitidas.

**Onde:** `security/config/SecurityConfig.java`.

**Por quê:** sem configuração de CORS, um front-end em outro domínio/porta será bloqueado pelo navegador. Ajustar conforme o ambiente de desenvolvimento/produção.

---

### C9. Escrever testes de autenticação

- [ ] Testes de unidade e integração para o fluxo de auth.

**Onde:** `src/test/java/.../security/`.

**Por quê:** hoje não há **nenhum** teste de auth (só `contextLoads`). Sugestões:
- `JwtServiceTest`: gerar token e extrair username; token expirado/assinatura errada lança exceção.
- `AuthControllerTest` (MockMvc): login sucesso/credenciais inválidas; register sucesso/duplicado/role fixado em USER.
- `JwtAuthenticationFilterTest`: token válido autentica; token inválido não autentica; usuário inexistente não estoura.

---

### C10. Versionar os endpoints de auth

- [ ] Decidir se `/auth/**` vira `/v1/auth/**`.

**Onde:** `security/auth/AuthController.java:22`.

**Por quê:** todos os outros recursos são `/v1/...`; auth está fora do versionamento. Decisão sua (já anotada para depois).

---

### C11. Pacote `config` contém um serviço (`JwtService`)

- [ ] Reavaliar a localização de `JwtService`.

**Onde:** `security/config/JwtService.java`.

**Por quê:** `JwtService` é um **serviço**, não configuração. Semanticamente ficaria melhor em `security/auth` ou em um pacote `security/jwt`. Puramente organizacional — você escolheu `config`, então fica a critério.

---

### C12. Renomear `authenticationManagerBean`

- [ ] Trocar para `authenticationManager`.

**Onde:** `security/config/SecurityConfig.java:39`.

**Por quê:** sufixo `Bean` é convenção antiga (Spring pré-3). O nome comum hoje é só `authenticationManager`. Cosmético.

---

## Ordem sugerida de execução

1. **P0:** A1, A2, A3, A4 (pequenas e de alto impacto) → depois A5.
2. **P1:** B1, B2, B3, B4, B5, B6, B7, B8, B9.
3. **P2:** C1, C2, C3, C9, C8 e, por último, as decisões de arquitetura (C4, C5, C6, C7, C10, C11, C12).

## Checklist resumido (copie para acompanhar)

```
Segurança crítica (P0)
[ ] A1. Registro público sempre USER (sem role do cliente)
[ ] A2. Remover default do segredo JWT (API_SECURITY_TOKEN_SECRET obrigatório)
[ ] A3. UNIQUE em username + tratar DataIntegrityViolationException
[ ] A4. Bean Validation em LoginRequest/RegisterRequest
[ ] A5. Rate limiting / lockout no login

Status HTTP e robustez (P1)
[ ] B1. Token inválido -> 401 (TokenInvalidoException)
[ ] B2. EntryPoint 401 / AccessDeniedHandler 403 em JSON
[ ] B3. Matcher /v1/atletas/** para ADMIN
[ ] B4. CustomUserDetailsService lança UsernameNotFoundException
[ ] B5. resolveToken valida prefixo "Bearer "
[ ] B6. Filtro não estoura NPE com usuário inexistente
[ ] B7. findByUsername -> Optional<User>
[ ] B8. register responde ApiError (e/ou AuthService)
[ ] B9. BadCredentialsException -> 401

Boas práticas (P2)
[ ] C1. Usar Clock do projeto no JwtService
[ ] C2. @ConfigurationProperties para as props do token
[ ] C3. Expiração/issuer configuráveis/constantes
[ ] C4. Avaliar separar UserDetails da entidade User
[ ] C5. Definir destino do campo email (@Email/unique ou remover)
[ ] C6. Refresh token / logout / revogação
[ ] C7. (Opcional) roles como claim do token
[ ] C8. Configurar CORS
[ ] C9. Testes de autenticação
[ ] C10. Versionar /auth
[ ] C11. Reavaliar pacote de JwtService
[ ] C12. Renomear authenticationManagerBean
```
