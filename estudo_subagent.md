# Study: agents, subagents, and per-task models in OpenCode V2

> Study document assembled from the official V2 documentation
> (`/docs/agents`, `/docs/models`, `/docs/commands`) and from the model
> catalog available on the `opencode-go` provider at the time of writing.
>
> **Status:** a proposal — nothing has been created yet. The final section
> lists the open decisions.

---

## Table of contents

1. [How to ask the agent to plan and dispatch subagents](#1-how-to-ask-the-agent-to-plan-and-dispatch-subagents)
2. [How to configure a specific model per task](#2-how-to-configure-a-specific-model-per-task)
3. [Proposed plan for this repository](#3-proposed-plan-for-this-repository)
4. [Open decisions](#4-open-decisions)

---

## 1. How to ask the agent to plan and dispatch subagents

It works on two layers: **in the prompt** (behavior) and **in the
configuration** (guarantee).

### 1.1 In the prompt

The reliable trigger is to ask for the plan **with the dispatch embedded**, and
to hold off execution until approval:

```text
Don't edit anything yet. Build a plan in numbered steps and, for each step,
state which subagent will execute it and which model. Wait for my approval.
```

After approval:

```text
Execute the plan. Use the explore subagent to locate the code and general
for the changes. When done, use the reviewer to review the diff.
```

Practices that change the outcome:

- **Name the subagent explicitly** (`explore`, `general`, `reviewer`). Without
  a name, the model picks one itself — and picks badly on a small task.
- **Parallelize explicitly**: "run A and B in parallel, then consolidate" —
  otherwise it serializes.
- Each subagent starts with a **zeroed context**: the briefing must be
  self-contained (paths, goal, definition of done).
- `general` **cannot** launch new subagents (built-in safety limit).

For a reusable command, create `.opencode/commands/planejar.md`:

```md
---
description: Plan before executing
agent: plan
---

Analyze $ARGUMENTS and produce a step-by-step plan. For each step, state the
subagent and the model. Don't change any files until I approve.
```

Usage: `/planejar team registration`.

### 1.2 In the configuration (make it ask before dispatching)

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

Closed-catalog version — it may only launch what has been authorized:

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

The most specific rule wins (the last one that matches), so `deny *` comes
first.

---

## 2. How to configure a specific model per task

There are four scopes, from broadest to narrowest:

| Scope | Where | Effect |
| --- | --- | --- |
| Session | `/models` or `opencode run --model ...` | only that session/run |
| Project global | `"model"` at the root of `opencode.jsonc` | default for new work |
| Per agent | `model:` on the agent | every task of that profile |
| Per command | `model:` on the command | that invocation |

**Precedence:** command model > model of the agent selected by the command >
session model. A subagent without a configured `model` **inherits the parent
session's model**.

### 2.1 One agent per task type (recommended approach)

`.opencode/agents/explore-codigo.md` — search only, cheap model:

```md
---
description: Locates files and symbols in the code without editing
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

Find what was requested and reply with file paths and lines.
Don't edit anything.
```

`.opencode/agents/arquiteto.md` — heavy reasoning:

```md
---
description: Drafts implementation plans and reviews architecture decisions
mode: subagent
model: opencode-go/deepseek-v4-pro#high
---

Produce implementation plans with steps, affected files, and risks.
```

`.opencode/agents/reviewer.md` — read-only review:

```md
---
description: Reviews the diff for bugs and regressions
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

Review the current changes. List findings by severity with file and line.
```

Format of `model`: `provider/model` with an optional variant (`#low`, `#high`,
`#max`). The valid variants come from the catalog — on the `opencode-go`
provider, `deepseek-v4-pro` has `high|max`, `glm-5.3` has `low|high|max`,
`qwen3.8-flash` has `none|low|medium|xhigh`. A nonexistent variant raises a
resolution error.

### 2.2 Alternative: everything in JSON

```jsonc
{
  "$schema": "https://opencode.ai/config.json",
  "model": "opencode-go/deepseek-v4.1-flash",
  "agents": {
    "arquiteto": {
      "mode": "subagent",
      "model": "opencode-go/deepseek-v4-pro#high",
      "description": "Drafts implementation plans"
    },
    "reviewer": {
      "mode": "subagent",
      "model": "opencode-go/glm-5.3#high",
      "description": "Reviews the diff for bugs and regressions"
    }
  }
}
```

### 2.3 Per command, when the task is one-off

```md
---
agent: plan
model: opencode-go/deepseek-v4-pro#high
subagent: true
---

Audit $ARGUMENTS.
```

`subagent: true` runs in a child session in the background; the parent stays
free and receives the result at the end.

### 2.4 Practical summary

- **Guarantee review before dispatch:**
  `{ "action": "subagent", "resource": "*", "effect": "ask" }`.
- **Ask for a plan:** numbered steps with the subagent and model per step, plus
  "don't edit until I approve".
- **Model per task:** agent in `.opencode/agents/` with `mode: subagent` +
  `model:`.
- **One-off model:** `opencode run --model opencode-go/deepseek-v4-pro#high "..."`.

Config changes in real time (the files are watched); if something doesn't
reload, use `/reload`.

---

## 3. Proposed plan for this repository

Context taken into account: Spring Boot 4.1 + Java 25, Flyway active with
`ddl-auto: validate`, MapStruct + Lombok coupled to `annotationProcessorPaths`,
JPA with H2 in tests, dense domain rules (registrations, matches,
standings) and JWT/security.

### 3.1 Principle: reuse built-ins before creating new ones

OpenCode already ships `explore` (read-only subagent) and `general`
(multi-step work subagent). Creating a parallel `explorer` would be
duplication: the same effect comes from **overriding the built-in with the
same ID** and only changing the model. New agents only where no equivalent
profile exists.

Result: **2 overrides + 3 new agents**.

### 3.2 The agents

| # | Agent | Type | Model | Why that model |
| --- | --- | --- | --- | --- |
| 1 | `explore` (override) | subagent, read-only | `opencode-go/qwen3.8-flash` | $0.15/$0.47 — search and reading are high-volume, low-reasoning work. No `#variant` (reasoning raises cost with no gain here) |
| 2 | `general` (override) | subagent | `opencode-go/glm-5.3#high` | $1.40/$4.40 — real implementation: multiple files, MapStruct/Lombok, refactoring. Good cost/benefit for code |
| 3 | `arquiteto` | subagent, read-only | `opencode-go/deepseek-v4-pro#high` | $0.66/$1.98 — design reasoning for less than 1/3 of `gemini-4-argon` or `kimi-k3.1-pro`. This is where mistakes are expensive |
| 4 | `revisor` | subagent, read-only | `opencode-go/kimi-k2.7-code#high` | $0.95/$4.00 — model tuned for code, good at diffs and regressions; cheaper on input than `glm-5.3` and `kimi-k3.1-pro` |
| 5 | `migrations` | subagent, restricted write | `opencode-go/deepseek-v4-pro#high` | Flyway is irreversible (`V22__...`, never edit an applied one). Same model as the architect for its specificity, but with `edit` allowed only in `db/migration/**` |

There is no separate testing agent: in this project the tests are pure
JUnit/Mockito + `@DataJpaTest` on H2, which `general` covers. A new agent
there would be cost without return — it can be promoted later if it becomes a
recurring task.

### 3.3 Permissions (the part that matters most)

- `explore`, `arquiteto`, `revisor`: `edit: * → deny` and `shell: * → deny`.
  A reviewer that edits stops reviewing and starts justifying its own code.
- `migrations`: `edit` allowed only in `src/main/resources/db/migration/**`,
  denied everywhere else. It is the only write exception among the
  specialists.
- `plan` (model override to `deepseek-v4-pro#high`) gets `subagent` with
  `effect: ask` — approval per dispatch.
- `build` (main agent, `deepseek-v4.1-flash`) also with `subagent: ask`
  in the first phase.

### 3.4 Intended dispatch flow

```text
build (deepseek-v4.1-flash)
  └─ plan + arquiteto  → step-by-step plan (human approval)
       └─ explore ×N    → locate services/entities/mappers in parallel
       └─ general       → implementation
       └─ migrations    → only if there is a new column/table
       └─ revisor       → final diff
```

The detail that motivates a cheap `explore` in parallel: almost every task
crosses 5–6 layers (entity → repository → service → controller → mapper →
DTO → test). That is 5–6 independent searches running at the same time —
exactly where a cheap model pays off.

### 3.5 Relative cost per task

Ordinal, not a promise: an average task (plan + 4 searches + implementation +
review) lands around **1× `general` + 1× `revisor` + noise** from the cheap
ones. Replacing everything with `claude-fable-5-2` ($8/$40) or `gpt-6-astra`
($10/$50) would multiply the cost by ~5–20 without changing the quality of
the outcome for this type of task.

### 3.6 Files that would be created (5)

```text
opencode.jsonc                       → default model, overrides (explore/general/plan),
                                       permissions, closed catalog
.opencode/agents/arquiteto.md
.opencode/agents/revisor.md
.opencode/agents/migrations.md
.opencode/commands/planejar.md       → /planejar <task> with agent: plan
```

`planejar.md` is optional, but it is what turns "I have to remember to ask for
a plan" into a one-word command.

### 3.7 Caveats

1. **The model map is a hypothesis, not a fact.** The most likely swaps after
   measuring: `revisor` to `glm-5.3#high` if it misses long context; `explore`
   to `claude-haiku-5-5` ($0.10/$0.50) if search with `qwen3.8-flash` drops
   files.
2. **The `#high` variant everywhere is a cost trap.** `explore` is kept
   without a variant on purpose.

---

## 4. Open decisions

- [ ] Approve the design of 2 overrides + 3 new agents, or slim it down
      (e.g.: drop `migrations` and use `general` with restricted permission).
- [ ] `general` on `glm-5.3#high` vs. `deepseek-v4-pro` (better reasoning,
      lower output cost, but slower).
- [ ] `revisor` on `kimi-k2.7-code#high` vs. `glm-5.3#high` (unifying models
      reduces the variables to measure).
- [ ] Whether to create `.opencode/commands/planejar.md`.
- [ ] Keep `subagent: ask` only in the initial phase or as a permanent
      default.
- [ ] Pick a real backlog task (e.g.: an item from
      `src/main/java/br/gov/quixada/esporte/iterations/pendencias.md`) to
      serve as a before/after comparison baseline.
