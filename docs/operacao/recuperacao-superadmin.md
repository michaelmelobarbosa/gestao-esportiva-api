# Recuperação de senha do SUPERADMIN

> **Operação de servidor.** Não existe endpoint HTTP para recuperação. O reset é feito por um comando no terminal do servidor, que exige acesso às credenciais do banco e às variáveis de ambiente da aplicação.
>
> Este documento **não contém segredos**: os valores abaixo são placeholders. Nunca coloque aqui senhas reais, o segredo do JWT, credenciais de banco ou hostnames internos.

## Contexto

- O sistema mantém **exatamente 1 SUPERADMIN**, responsável por gerenciar os `ADMIN`.
- A API não permite criar nem promover outro `SUPERADMIN`.
- A recuperação é feita pelo `SuperadminPasswordResetRunner` (profile `superadmin-reset`), que:
  1. valida o e-mail informado e exige que o usuário tenha papel `SUPERADMIN`;
  2. re-hasheia a nova senha com BCrypt;
  3. reativa a conta (`status = ATIVO`);
  4. registra a auditoria `PASSWORD_RESET` (ator `system:cli`);
  5. encerra o processo com código `0` (sucesso) ou `1` (falha).

## Pré-requisitos

- JDK 25 (`java -version`).
- Banco de dados acessível.
- As mesmas variáveis do boot normal da aplicação:
  - `API_SECURITY_TOKEN_SECRET` (mínimo 32 caracteres);
  - credenciais do banco (`MYSQL_USER`/`MYSQL_PASSWORD` no perfil `dev`, ou `SPRING_DATASOURCE_*` em produção).
- Permissão de acesso ao shell do servidor.

## 1. Gerar o artefato

```bash
./mvnw package -DskipTests
```

O jar é gerado em `target/gestao-esportiva-api-0.0.1-SNAPSHOT.jar`.

## 2. Modo interativo (recomendado)

```bash
java -jar target/gestao-esportiva-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=dev,superadmin-reset
```

O terminal solicita a nova senha (sem exibir na tela):

```
Nova senha do SUPERADMIN:
Confirme a nova senha:
```

Ao final:

```
Senha do SUPERADMIN 'admin@quixada.gov.br' redefinida com sucesso.
```

O processo encerra sozinho. `0` = sucesso; `1` = falha.

## 3. Modo não interativo

Para automação ou ambientes sem console interativo:

```bash
SUPERADMIN_NEW_PASSWORD='<NOVA_SENHA>' \
  java -jar target/gestao-esportiva-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=dev,superadmin-reset
```

> Se houver console disponível, o valor de `SUPERADMIN_NEW_PASSWORD` é ignorado (o console tem prioridade). **Nunca** passe a senha como argumento de linha de comando (`--senha=...`), pois ela apareceria em `ps` e no histórico do shell.

## 4. E-mail alvo

O padrão é o e-mail do seed. Para outro e-mail:

```bash
# via argumento
java -jar target/gestao-esportiva-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=dev,superadmin-reset \
  --reset.email=<EMAIL_DO_SUPERADMIN>

# ou via variável de ambiente
SUPERADMIN_EMAIL=<EMAIL_DO_SUPERADMIN> \
  java -jar target/gestao-esportiva-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=dev,superadmin-reset
```

Precedência: `--reset.email` > `SUPERADMIN_EMAIL` > e-mail padrão do seed.
O e-mail informado precisa existir e ter papel `SUPERADMIN`, senão o comando falha.

## 5. Produção

`application-prod.yaml` ainda não existe neste projeto. Aponte o datasource por variáveis de ambiente (exemplo com placeholders):

```bash
SPRING_PROFILES_ACTIVE=prod,superadmin-reset \
SPRING_DATASOURCE_URL='jdbc:mysql://<DB_HOST>:3306/<DB_NAME>?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=America/Recife' \
SPRING_DATASOURCE_USERNAME='<DB_USER>' \
SPRING_DATASOURCE_PASSWORD='<DB_PASSWORD>' \
API_SECURITY_TOKEN_SECRET='<JWT_SECRET>' \
  java -jar /caminho/gestao-esportiva-api-0.0.1-SNAPSHOT.jar
```

O profile `superadmin-reset` usa porta efêmera (`server.port: 0`) e desliga o Docker Compose; o processo sobe, executa o reset e encerra.

## 6. Depois do reset

Faça login normalmente e confirme o acesso:

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<EMAIL_DO_SUPERADMIN>","password":"<NOVA_SENHA>"}'
```

A ação fica registrada em `db_user_audit` com `action = PASSWORD_RESET`, ator `system:cli` e o alvo redefinido. Ela pode ser consultada em `GET /v1/users/audit` (requer token de `SUPERADMIN`).

## Limitações

- **JWT é stateless:** tokens emitidos antes do reset continuam válidos até expirarem (padrão `2h`). Revogação imediata não está implementada (item C6 do roadmap).
- O comando **não cria** usuário. Se não houver `SUPERADMIN` com o e-mail informado, ele falha.
- O reset **reativa** a conta, mesmo que ela estivesse inativa.

## Boas práticas

- Rotacione a senha do `SUPERADMIN` imediatamente após o primeiro deploy (o seed é público no histórico de migrations).
- Restrinja o acesso ao shell do servidor ao menor número de operadores possível.
- Revise periodicamente a auditoria de usuários.
- Não registre a senha nem o segredo do JWT em logs, tickets ou histórico de shell.

## Troubleshooting

| Mensagem | Causa |
|---|---|
| `Falha ao redefinir a senha: Senha deve ter entre 8 e 72 caracteres` | Senha fora do intervalo permitido |
| `Falha ao redefinir a senha: SUPERADMIN não encontrado para o e-mail informado` | E-mail inexistente ou usuário sem papel `SUPERADMIN` |
| `Senha não informada. Use o console interativo ou a variável SUPERADMIN_NEW_PASSWORD.` | Sem console interativo e sem a variável de ambiente |
| `As senhas não conferem.` | Confirmação diferente da senha digitada |
| Falha de conexão / Flyway | Banco inacessível ou credenciais/variáveis de ambiente incorretas |

## Nunca inclua neste documento

- Senha padrão do seed ou qualquer senha real.
- Valor de `API_SECURITY_TOKEN_SECRET`.
- Usuários/senhas de banco.
- Hostnames, IPs ou URLs internas de produção.
