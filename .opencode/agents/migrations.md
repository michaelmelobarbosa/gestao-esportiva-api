---
description: Creates Flyway migrations only in db/migration, without editing applied ones
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
Create only new migrations `V{N}__*.sql` with the next free number (the latest is `V22`). Never edit an applied migration; `ddl-auto` is `validate`.
