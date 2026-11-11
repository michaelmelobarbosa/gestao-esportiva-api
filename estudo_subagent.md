# Estudo: agentes, subagents e modelos por tarefa no OpenCode V2

> Documento de estudo montado a partir da documentação oficial V2
> (`/docs/agents`, `/docs/models`, `/docs/commands`) e do catálogo de modelos
> disponível no provider `opencode-go` no momento da escrita.
>
> **Status:** proposta, nada foi criado ainda. A seção final lista as decisões
> em aberto.

---

## Sumário

1. [Como pedir para o agente planejar e disparar subagents](#1-como-pedir-para-o-agente-planejar-e-disparar-subagents)
2. [Como configurar um modelo específico por tarefa](#2-como-configurar-um-modelo-específico-por-tarefa)
3. [Plano proposto para este repositório](#3-plano-proposto-para-este-repositório)
4. [Decisões em aberto](#4-decisões-em-aberto)

---

## 1. Como pedir para o agente planejar e disparar subagents

Funciona em duas camadas: **no prompt** (comportamento) e **na configuração**
(garantia).

### 1.1 No prompt

O gatilho confiável é pedir o plano **com o disparo embutido**, e travar a
execução até haver aprovação:

```text
Não edite nada ainda. Monte um plano em etapas numeradas e, para cada etapa,
diga qual subagent vai executar e qual modelo. Aguarde minha aprovação.
```

Depois da aprovação:

```text
Execute o plano. Use o subagent explore para localizar o código e o general
para as mudanças. Ao terminar, use o reviewer para revisar o diff.
```

Boas práticas que mudam o resultado:

- **Cite o subagent pelo nome** (`explore`, `general`, `reviewer`). Sem nome, o
  modelo escolhe sozinho — e escolhe mal em tarefa pequena.
- **Paralelize explicitamente**: "rode em paralelo A e B, depois consolide" —
  senão ele serializa.
- Cada subagent começa com **contexto zerado**: o briefing precisa ser
  autocontido (caminhos, objetivo, critério de pronto).
- `general` **não** pode lançar novos subagents (limite de segurança embutido).

Para um comando reutilizável, criar `.opencode/commands/planejar.md`:

```md
---
description: Planeja antes de executar
agent: plan
---

Analise $ARGUMENTS e produza um plano em etapas. Para cada etapa, informe o
subagent e o modelo. Não altere arquivos até eu aprovar.
```

Uso: `/planejar inscrição de equipe`.

### 1.2 Na configuração (garantir que ele pergunte antes de disparar)

```jsonc
{
  "agents": {
    "build": {
      "permissions": [
        { "action": "subagent", "resource": "*", "effect": "ask" }
      ]
    }
  }
}
```

Versão com catálogo fechado — só pode lançar o que for autorizado:

```jsonc
{
  "agents": {
    "build": {
      "permissions": [
        { "action": "subagent", "resource": "*", "effect": "deny" },
        { "action": "subagent", "resource": "explore", "effect": "allow" },
        { "action": "subagent", "resource": "reviewer", "effect": "allow" }
      ]
    }
  }
}
```

A regra mais específica vence (a última que casa), então `deny *` vem antes.

---

## 2. Como configurar um modelo específico por tarefa

Existem quatro escopos, do mais amplo ao mais estreito:

| Escopo | Onde | Efeito |
| --- | --- | --- |
| Sessão | `/models` ou `opencode run --model ...` | só aquela sessão/execução |
| Global do projeto | `"model"` na raiz do `opencode.jsonc` | default de novos trabalhos |
| Por agente | `model:` no agente | toda tarefa daquele perfil |
| Por comando | `model:` no comando | aquela invocação |

**Precedência:** modelo do comando > modelo do agente selecionado pelo comando >
modelo da sessão. Subagent sem `model` configurado **herda o modelo da sessão
pai**.

### 2.1 Um agente por tipo de tarefa (jeito recomendado)

`.opencode/agents/explore-codigo.md` — só busca, modelo barato:

```md
---
description: Localiza arquivos e símbolos no código sem editar
mode: subagent
model: opencode-go/qwen3.8-flash
permissions:
  - action: edit
    resource: "*"
    effect: deny
  - action: shell
    resource: "*"
    effect: deny
---

Encontre o que foi pedido e responda com caminhos de arquivo e linhas.
Não edite nada.
```

`.opencode/agents/arquiteto.md` — raciocínio pesado:

```md
---
description: Desenha planos de implementação e revisa decisões de arquitetura
mode: subagent
model: opencode-go/deepseek-v4-pro#high
---

Produza planos de implementação com etapas, arquivos afetados e riscos.
```

`.opencode/agents/reviewer.md` — revisão somente leitura:

```md
---
description: Revisa o diff por bugs e regressões
mode: subagent
model: opencode-go/glm-5.3#high
permissions:
  - action: edit
    resource: "*"
    effect: deny
  - action: shell
    resource: "*"
    effect: deny
---

Revise as mudanças atuais. Liste achados por severidade com arquivo e linha.
```

Formato do `model`: `provider/model` com variante opcional (`#low`, `#high`,
`#max`). As variantes válidas vêm do catálogo — no provider `opencode-go`,
`deepseek-v4-pro` tem `high|max`, `glm-5.3` tem `low|high|max`,
`qwen3.8-flash` tem `none|low|medium|xhigh`. Variante inexistente dá erro de
resolução.

### 2.2 Alternativa: tudo em JSON

```jsonc
{
  "$schema": "https://opencode.ai/config.json",
  "model": "opencode-go/deepseek-v4.1-flash",
  "agents": {
    "arquiteto": {
      "mode": "subagent",
      "model": "opencode-go/deepseek-v4-pro#high",
      "description": "Desenha planos de implementação"
    },
    "reviewer": {
      "mode": "subagent",
      "model": "opencode-go/glm-5.3#high",
      "description": "Revisa o diff por bugs e regressões"
    }
  }
}
```

### 2.3 Por comando, quando a tarefa é pontual

```md
---
agent: plan
model: opencode-go/deepseek-v4-pro#high
subagent: true
---

Audite $ARGUMENTS.
```

`subagent: true` roda em sessão filha em background; o pai continua livre e
recebe o resultado ao final.

### 2.4 Resumo prático

- **Garantir revisão antes do disparo:**
  `{ "action": "subagent", "resource": "*", "effect": "ask" }`.
- **Pedir plano:** etapas numeradas com subagent e modelo por etapa, e "não edite
  até eu aprovar".
- **Modelo por tarefa:** agente em `.opencode/agents/` com `mode: subagent` +
  `model:`.
- **Modelo pontual:** `opencode run --model opencode-go/deepseek-v4-pro#high "..."`.

Config muda em tempo real (os arquivos são observados); se algo não recarregar,
usar `/reload`.

---

## 3. Plano proposto para este repositório

Contexto considerado: Spring Boot 4.1 + Java 25, Flyway ativo com
`ddl-auto: validate`, MapStruct + Lombok acoplados ao `annotationProcessorPaths`,
JPA com H2 em test, regras de domínio densas (inscrições, partidas,
classificação) e JWT/segurança.

### 3.1 Princípio: reaproveitar built-ins antes de criar novos

O OpenCode já traz `explore` (subagent read-only) e `general` (subagent de
trabalho multi-etapa). Criar um `explorador` paralelo seria duplicação: o mesmo
efeito vem de **sobrescrever o built-in com o mesmo ID** e só mudar o modelo.
Agentes novos só onde não existe perfil equivalente.

Resultado: **2 overrides + 3 agentes novos**.

### 3.2 Os agentes

| # | Agente | Tipo | Modelo | Por quê esse modelo |
| --- | --- | --- | --- | --- |
| 1 | `explore` (override) | subagent, read-only | `opencode-go/qwen3.8-flash` | $0,15/$0,47 — busca e leitura são volume alto e baixo raciocínio. Sem `#variante` (reasoning encarece sem ganho aqui) |
| 2 | `general` (override) | subagent | `opencode-go/glm-5.3#high` | $1,40/$4,40 — implementação real: múltiplos arquivos, MapStruct/Lombok, refactor. Bom custo/benefício para código |
| 3 | `arquiteto` | subagent, read-only | `opencode-go/deepseek-v4-pro#high` | $0,66/$1,98 — raciocínio de projeto por menos de 1/3 do `gemini-4-argon` ou `kimi-k3.1-pro`. É onde errar sai caro |
| 4 | `revisor` | subagent, read-only | `opencode-go/kimi-k2.7-code#high` | $0,95/$4,00 — modelo afinado para código, bom em diff e regressão; mais barato na entrada que `glm-5.3` e que `kimi-k3.1-pro` |
| 5 | `migracoes` | subagent, escrita restrita | `opencode-go/deepseek-v4-pro#high` | Flyway é irreversível (`V21__...`, nunca editar aplicada). Mesmo modelo do arquiteto pela especificidade, mas com `edit` liberado só em `db/migration/**` |

Não há agente de testes separado: no projeto os testes são JUnit/Mockito puros +
`@DataJpaTest` em H2, o que o `general` cobre. Agente novo aí seria custo sem
retorno — dá para promover depois se virar tarefa recorrente.

### 3.3 Permissões (o ponto que mais importa)

- `explore`, `arquiteto`, `revisor`: `edit: * → deny` e `shell: * → deny`. Um
  revisor que edita deixa de revisar e passa a justificar o próprio código.
- `migracoes`: `edit` liberado só em `src/main/resources/db/migration/**`,
  negado no resto. É a única exceção de escrita entre os especialistas.
- `plan` (override de modelo para `deepseek-v4-pro#high`) recebe `subagent` com
  `effect: ask` — aprovação por disparo.
- `build` (agente principal, `deepseek-v4.1-flash`) também com `subagent: ask`
  na primeira fase.

### 3.4 Fluxo de despacho pretendido

```text
build (deepseek-v4.1-flash)
  └─ plan + arquiteto  → plano em etapas (aprovação humana)
       └─ explore ×N    → localizar serviços/entidades/mappers em paralelo
       └─ general       → implementação
       └─ migracoes     → só se houver nova coluna/tabela
       └─ revisor       → diff final
```

O detalhe que motiva `explore` barato em paralelo: quase toda tarefa cruza 5–6
camadas (entity → repository → service → controller → mapper → DTO → teste). São
5–6 buscas independentes que rodam ao mesmo tempo — exatamente onde modelo
barato rende.

### 3.5 Custo relativo por tarefa

Ordinal, não promessa: uma tarefa média (plano + 4 buscas + implementação +
revisão) sai na faixa de **1× `general` + 1× `revisor` + ruído** dos baratos.
Substituir tudo por `claude-fable-5-2` ($8/$40) ou `gpt-6-astra` ($10/$50)
multiplicaria por ~5–20 sem mudar a qualidade do resultado neste tipo de tarefa.

### 3.6 Arquivos que seriam criados (5)

```text
opencode.jsonc                       → model default, overrides (explore/general/plan),
                                       permissões, catálogo fechado
.opencode/agents/arquiteto.md
.opencode/agents/revisor.md
.opencode/agents/migracoes.md
.opencode/commands/planejar.md       → /planejar <tarefa> com agent: plan
```

`planejar.md` é opcional, mas é o que transforma "eu tenho que lembrar de pedir
plano" em um comando de uma palavra.

### 3.7 Ressalvas

1. **Mapa de modelos é hipótese, não fato.** As trocas mais prováveis depois de
   medir: `revisor` para `glm-5.3#high` se ele errar contexto longo; `explore`
   para `claude-haiku-5-5` ($0,10/$0,50) se a busca com `qwen3.8-flash` perder
   arquivos.
2. **Variante `#high` em tudo é armadilha de custo.** Mantido sem variante no
   `explore` de propósito.

---

## 4. Decisões em aberto

- [ ] Aprovar o desenho de 2 overrides + 3 agentes novos, ou enxugar (ex.:
      dispensar `migracoes` e usar `general` com permissão restrita).
- [ ] `general` em `glm-5.3#high` vs. `deepseek-v4-pro` (raciocínio melhor,
      custo menor na saída, mas mais lento).
- [ ] `revisor` em `kimi-k2.7-code#high` vs. `glm-5.3#high` (unificar modelos
      reduz variáveis a medir).
- [ ] Criar ou não `.opencode/commands/planejar.md`.
- [ ] Manter `subagent: ask` só na fase inicial ou como padrão permanente.
- [ ] Definir uma tarefa real do backlog (ex.: item de
      `src/main/java/br/gov/quixada/esporte/iterations/pendencias.md`) para
      servir de baseline de comparação antes/depois.
