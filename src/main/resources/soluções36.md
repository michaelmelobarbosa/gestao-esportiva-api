# Soluções — Ponto 3.6: `update()` silencioso sobre CPF

> Origem: `src/main/resources/melhorias-atleta.md` §3.6 | Gerado em 2026-09-12
> Arquivos envolvidos: `atleta/AtletaService.java:86-97`, `atleta/dto/AtletaUpdateRequest.java`, `atleta/AtletaMapper.java:23-26`, `atleta/Atleta.java:50-52,74`, `atleta/Atleta.java:33`

## 1. O problema

`AtletaService.update()` copia manualmente 5 campos:

```java
atletaExistente.setNomeCompleto(atletaParaAtualizar.getNomeCompleto());
atletaExistente.setTelefone(atletaParaAtualizar.getTelefone());
atletaExistente.setDataNascimento(atletaParaAtualizar.getDataNascimento());
atletaExistente.setEndereco(atletaParaAtualizar.getEndereco());
atletaExistente.setSexo(atletaParaAtualizar.getSexo());
return atletaExistente;
```

`cpf` não é copiado → na prática é imutável. Correto, mas por **omissão**, não por garantia:

1. `AtletaUpdateRequest` nem tem campo `cpf` — o cliente não consegue enviar, mas nada documenta isso.
2. `AtletaMapper.toEntity(UpdateRequest)` não tem `@Mapping(target="cpf", ignore=true)` explícito — funciona por acidente (item 4.3).
3. `Atleta.cpf` é `@Column(unique, nullable=false)` mas **sem** `updatable=false` (diferente de `dataCadastro`, que já tem). Nada impede um `setCpf()` futuro de persistir.
4. `@Setter` aberto na entidade permite `setCpf()` de qualquer lugar (item 4.1).
5. `update()` não checa `status` — atleta `INATIVO` pode ser editado livremente.

São dois subproblemas: **(a)** CPF imutável mas silencioso; **(b)** edição de inativo liberada.

## 2. Alternativas

### A. Documental / mínima (menor risco — passo 1 recomendado)

Manter o comportamento, tornar a intenção explícita:

- Javadoc em `update()`: "CPF imutável após criação".
- `@Mapping(target="cpf", ignore=true)` + `id, status, dataCadastro` explícitos no `toEntity(UpdateRequest)` (resolve o item 4.3 junto).
- Descrição OpenAPI no `PUT /{id}`: "CPF não é alterável".
- Teste: `update` preserva o `cpf` original.

Prós: zero quebra, didático, 1 commit pequeno.
Contras: não impede edição de inativo; não blinda contra `setCpf()` futuro.

### B. Defesa em profundidade na entidade

- Adicionar `@Column(updatable=false)` no `cpf` (igual ao já feito em `dataCadastro`).
- Evoluir para remover `@Setter` geral (item 4.1) e criar método de domínio, ex. `atleta.atualizarDados(nome, tel, dataNasc, end, sexo)`.

Prós: mesmo se alguém chamar `setCpf()`, o JPA ignora no `UPDATE`; regra sai do Service e vai para o modelo.
Contras: falha **silenciosa** por natureza (JPA não avisa) — combinar com A para dar feedback; `updatable=false` exige atenção ao schema (`ddl-auto:update` mascara isso hoje).

### C. Bloqueio total de update em `INATIVO` (mais restritiva)

```java
if (atletaExistente.getStatus() == StatusAtleta.INATIVO)
    throw new AtletaInativoException("Reative antes de editar");
```

Nova exception `422/409` + handler em `GlobalExceptionHandler` (conecta com o item 7, hoje só trata 2 exceções).

Prós: integridade forte, regra clara; evita "ressuscitar dados" de cadastro morto.
Contras: UX pior — corrigir um telefone exige `PATCH /ativar → PUT → PATCH /inativar`; precisa decisão de produto; exige novo teste.

### D. Whitelist para inativo (meio-termo)

Permite `PUT` em `INATIVO` só para campos "cadastrais inofensivos" (`telefone, endereco`) e bloqueia `nome, dataNascimento, sexo` (ou vice-versa).

Prós: flexível.
Contras: regra inventada, complexa de documentar/testar; sem requisito do negócio, é over-engineering agora. Não recomendada sem demanda real.

### E. Fail-fast se cliente tentar trocar CPF

Adicionar `cpf` opcional no `AtletaUpdateRequest` e rejeitar `400` se vier diferente do persistido, ou criar endpoint separado `PATCH /{id}/cpf` com fluxo de verificação.

Prós: feedback explícito em vez de ignorar silenciosamente.
Contras: expande a API sem necessidade hoje; `PUT` com CPF no payload confunde ("posso trocar?"); só faz sentido se houver caso de uso real de correção de CPF digitado errado.

## 3. Plano de estudo / implementação sugerido

1. Faça **A agora** (documentar + `ignore=true` + teste) — resolve 80% do 3.6 sem risco.
2. Faça **B** junto com o item 4.1 (`@Setter` → métodos de domínio) — são o mesmo refactor.
3. Decida **C vs. manter liberado** com regra de negócio: hoje nada indica que inativo não possa ser editado. Se optar por C, crie `AtletaInativoException` já no padrão `ProblemDetail` do item 7 para não criar handler descartável.

## 4. Referências no código

- `src/main/java/br/gov/quixada/esporte/atleta/AtletaService.java:86-97` — `update()`
- `src/main/java/br/gov/quixada/esporte/atleta/dto/AtletaUpdateRequest.java:13-28` — sem `cpf`
- `src/main/java/br/gov/quixada/esporte/atleta/AtletaMapper.java:23-26` — falta `ignore=true` explícito
- `src/main/java/br/gov/quixada/esporte/atleta/Atleta.java:50-52` — `cpf` sem `updatable=false`
- `src/main/java/br/gov/quixada/esporte/atleta/Atleta.java:74` — `dataCadastro` com `updatable=false` (exemplo a seguir)
- `src/main/java/br/gov/quixada/esporte/atleta/Atleta.java:33` — `@Setter` aberto
- `src/main/java/br/gov/quixada/esporte/exceptions/GlobalExceptionHandler.java` — só 2 exceções tratadas
