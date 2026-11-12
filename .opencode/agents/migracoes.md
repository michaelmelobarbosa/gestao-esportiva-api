---
description: Cria migrations Flyway somente em db/migration, sem editar as já aplicadas
mode: subagent
model: opencode-go/deepseek-v4-pro#high
permissions:
  - action: edit
    resource: "*"
    effect: deny
  - action: edit
    resource: "src/main/resources/db/migration/**"
    effect: allow
  - action: shell
    resource: "*"
    effect: deny
---
Crie apenas novas migrations V{N}__*.sql com o próximo número livre (a última é V21). Nunca edite uma migration já aplicada; ddl-auto é validate.
