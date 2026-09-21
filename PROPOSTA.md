# Proposta do Comitê de CI — API Java 21 + Maven

> Resposta à missão `README.md:19-31` e ao desafio extra `README.md:107-113`. Pipeline implementada em `.github/workflows/ci.yaml:1` e governança em `.github/BRANCH_PROTECTION.md:1`.

## 1. Diagnóstico do Cenário `README.md:9-17`

| Problema | Causa raiz na pipeline inicial | Risco |
|---|---|---|
| Código que não compila chega ao PR/`main` | `.github/workflows/ci.yaml:22-25` só faz `clean compile` + `package -DskipTests`, sem `test`/`verify`; sem `branch protection` | Quebra `main`, bloqueia time |
| Testes lentos (22min) | Job único serial, sem `cache: maven`, sem paralelismo | Feedback tardio, devs evitam rodar CI |
| Defesa de liberar JAR com teste falho | `package -DskipTests` ignora resultado de testes; `upload-artifact: api` genérico sempre publica | Artefato não confiável em produção |
| Artefatos locais sem garantia | Sem validação de `javaVersion:21` nem de commit; deploy manual | Não reprodutível, “funciona na minha máquina” |
| PR mergeado com pipeline vermelha | `on: push [main,develop]` sem `pull_request`; sem `required_status_checks` | Bypassa qualidade |
| Sem rastreabilidade artefato→commit | `name: api` sem SHA, sem `MANIFEST`/`commit.txt`, sem provenance | Impossível auditar o que está em produção |
| Pipeline lenta mas sem cortes | Sem cache/paralelismo, tudo sequencial | Pressão para remover testes |

Projeto confirma separação correta em `pom.xml:52-80`: `maven-surefire-plugin` roda `*Test.java` (`mvn test`) e `maven-failsafe-plugin` roda `*IT.java` (`mvn verify`). A API expõe `GET /status` e `POST /releases/validate` com `commit`, `javaVersion`, `unitTestsPassed`, `integrationTestsPassed`.

---

## 2. Diagrama da Pipeline Proposta (Entrega 1)

### Fluxo P0 implementado

```mermaid
flowchart TD
    A[Push em main / Pull Request -> main] --> B[Checkout + setup-java 21 + cache maven]
    B --> C[Job compile: mvn -B clean compile]
    C -->|falha: bloqueia| F1[Falha - PR bloqueado]
    C -->|ok| D[Job unit-tests: mvn -B test]
    C -->|ok| E[Job integration-tests: mvn -B verify]
    D -->|falha| F1
    E -->|falha| F1
    D --> G{Ambos verdes?}
    E --> G
    G -->|nao| F1
    G -->|sim| H[Job package: mvn -B package]
    H --> I[Publicar api-${SHA}.jar + commit.txt]
    I --> J[Attest provenance - apenas em main]
    J --> K[Merge liberado - Branch Protection]

    F1 -.-> R1[Upload surefire-reports / failsafe-reports - retention 3d - apenas diagnostico]
```

**Leitura:** `compile` é o portão mais barato. `unit` e `integration` rodam **em paralelo** após `compile` (`needs: compile` em `.github/workflows/ci.yaml:39,65`). `package` só roda se ambos verdes (`needs: [unit-tests, integration-tests]` em `:91`). Nenhum JAR publicável é gerado em caso de falha.

### Fluxo otimizado para o desafio 22min (P3)

```
PR (feedback < 3min)              main (completo ~7-8min)
 compile (1min)                    compile (1min)
   |-> unit (1-2min)  ─┐            |-> unit (1-2min)  ─┐
   |-> integration (3-5min) ─> package   |-> integration (3-5min) ─> package + provenance
 cache maven hit ~70%              cache maven hit ~70%
 concurrency: cancela runs antigos do mesmo ref
```

---

## 3. Regras do Processo (Entrega 2)

### 3.1 Etapas e ordem `README.md:22-23`

1. `checkout` → 2. `setup-java 21 + cache: maven` → 3. `compile` → 4. `unit-tests` + `integration-tests` (paralelos) → 5. `package` → 6. `publish` → 7. `provenance`

Ordenadas por **custo crescente**: falhar em `compile` (5s) é mais barato que falhar em `IT` (30s+). Fail-fast evita desperdício.

### 3.2 Bloqueio `README.md:24`

- **Bloqueantes:** `compile`, `unit-tests`, `integration-tests`. Qualquer `exit != 0` falha o workflow; `package` nem inicia (dependência `needs`).
- **Não bloqueante diagnóstico:** `upload-artifact` de `surefire-reports`/`failsafe-reports` com `if: failure()` e `retention-days: 3` — não libera JAR, só logs para debug. Responde `README.md:102`.

### 3.3 Artefato: quando gerar e quando é publicável `README.md:25`

- **Gerar:** apenas após `test` + `verify` verdes, com `mvn -B package` **sem** `-DskipTests` (`.github/workflows/ci.yaml:105`). Antes era `package -DskipTests` (`ci.yaml` antigo `:25`) — removido.
- **Publicável:** somente artefato `api-${{ github.sha }}` (`:118`) vindo da CI em `main` com provenance. Artefato de `pull_request` tem `retention-days: 14` mas **não** é promovido a produção; é efêmero para revisão.
- **Não publicável:** JAR gerado localmente ou após falha — rejeitado no deploy. Validação do payload `POST /releases/validate` deve checar `javaVersion == 21` e `unitTestsPassed && integrationTestsPassed`.

### 3.4 Unit vs Integração `README.md:26`

**Separados, em jobs paralelos após compile.**

- `pom.xml:52-62` Surefire `**/*Test.java` → `mvn test` (rápido, sem Spring)
- `pom.xml:64-80` Failsafe `**/*IT.java` → `mvn verify` (sobe `StatusControllerIT`, `ReleaseValidationControllerIT`)

Por que não juntos? Unit dá feedback em ~15s-2min; IT sobe Tomcat e leva 5-6s por classe + contexto. Juntos somam; paralelos custam `max(unit, IT)` e permitem `concurrency` cancelar IT se unit já falhou em re-run. Responde `README.md:100`.

### 3.5 Verificações obrigatórias antes do merge `README.md:27`

Todos os 4 jobs como `required_status_checks` em `.github/BRANCH_PROTECTION.md:13-19`:

- `Compile`
- `Unit Tests`
- `Integration Tests`
- `Package & Publish`

+ `Require branches to be up to date` + `Dismiss stale approvals` + `Do not allow bypassing`. Sem isso `README.md:15,101` continua.

### 3.6 Artefatos locais `README.md:28`

**Não aceitos** para produção. Apenas `api-<sha>.jar` baixado via `gh run download` ou `actions/download-artifact` com `commit.txt` validado. Documentado no `BRANCH_PROTECTION.md:29`.

### 3.7 Rastreabilidade `README.md:29`

- Nome `api-${{ github.sha }}` (`:118`) + `if-no-files-found: error` (`:120`)
- `target/commit.txt` com `sha`, `run_id-run_number`, `ref` (`:108-113`)
- `actions/attest-build-provenance@v2` em `main` (`:123-126`) — garante que o JAR auditado veio da execução registrada
- Futuro: `maven-git-commit-id-plugin` para injetar SHA no `MANIFEST.MF`/`/status`

### 3.8 Redução de tempo sem remover verificações `README.md:30,107-113`

| Técnica | Onde em `ci.yaml` | Ganho estimado |
|---|---|---|
| `setup-java cache: maven` | `:32,50,76,102` | -8 a -12min (evita baixar `~/.m2`) |
| Jobs `unit`/`integration` paralelos | `:39,65` + `needs: compile` | -40% wall time |
| `concurrency.cancel-in-progress` | `:15-17` | -fila em push --force |
| `retention-days` diferenciado | `:61,87,121` | -armazenamento/custo |
| `pull_request` + `push main` (não todo push) | `:4-7` | -execuções desnecessárias |

Pipeline de 22min → ~7-8min sem cortar nenhum `*Test`/`*IT`. Pipeline rápida **não** é mais importante que completa (`README.md:105`): completude é garantida, velocidade vem de infra.

---

## 4. YAML da Pipeline (Entrega 3)

Implementado em `.github/workflows/ci.yaml:1-127` (commit `124637b`):

```yaml
name: CI
on:
  push: { branches: [main] }
  pull_request: { branches: [main] }
permissions: { contents: read, attestations: write, id-token: write }
concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: true }
jobs:
  compile:       # mvn clean compile, cache: maven
  unit-tests:    # needs: compile, mvn test, if: failure() -> surefire-reports 3d
  integration-tests: # needs: compile, mvn verify, if: failure() -> failsafe-reports 3d
  package:       # needs: [unit-tests, integration-tests], mvn package, api-${sha} 14d, provenance em main
```

Diferença para o ponto de partida (`.github/workflows/ci.yaml` antigo `:22-31`):

- Antes: 1 job serial, `package -DskipTests`, `name: api` genérico, `on: push [main,develop]` sem PR.
- Agora: 4 jobs com `needs`, `cache: maven`, `pull_request`, `api-${sha}`, `commit.txt`, `provenance`.

---

## 5. Justificativa Curta (Entrega 4) — Por que não alternativas

- **Por que não gerar JAR com teste falho?** `README.md:13,99,102` Viola “build once, test before publish”. JAR não testado em produção já causou incidentes no cenário; diagnóstico deve ser via `surefire-reports`, não binário. Decisão: `package` depende de `needs` verdes.

- **Por que não IT em todo push?** `README.md:100` IT consome 5-6s por classe + Tomcat (`ReleaseValidationControllerIT:64471`). Rodar em `push` de feature branch sem PR desperdiça 22min. PR e `main` são os portões onde IT é obrigatório.

- **Por que PR não pode mergear com pipeline vermelha?** `README.md:15,101` Confiar em disciplina humana falha sob pressão. `branch protection` com `required_status_checks` é barreira automática; `Do not allow bypassing` remove exceção de admin.

- **Por que nome com commit?** `README.md:16,104` Sem SHA, `api.jar` sobrescreve e impede `git bisect`/`rollback`. `api-<sha>.jar` + `commit.txt` permite `POST /releases/validate {commit, javaVersion}` validar que o deploy é exatamente o que passou na CI.

- **Por que não remover verificações para ganhar velocidade?** `README.md:17` Remover testes move custo para produção (hotfix, SLA). Cache + paralelismo atacam a causa (infra/rede), não a qualidade. Ganho de ~60% sem perder cobertura.

---

## 6. Status das Entregas

| Entrega `README.md:35-39` | Status | Arquivo |
|---|---|---|
| 1. Diagrama | ✅ Entregue | Este doc §2 (Mermaid) |
| 2. Regras do processo | ✅ Entregue | Este doc §3 + `.github/BRANCH_PROTECTION.md:1` |
| 3. YAML | ✅ Implementado e commitado | `.github/workflows/ci.yaml:1` (`124637b`) |
| 4. Justificativa | ✅ Entregue | Este doc §5 |
| 5. Apresentação 8-10min | 🔜 Roteiro abaixo | — |

### P0 (já commitado `124637b`)

- [x] Reescrita do YAML com 4 jobs bloqueantes, `cache: maven`, `needs`, `api-sha`, `provenance`
- [x] Branch protection documentada
- [x] Validação local: `mvn test 13 OK`, `mvn verify 2 IT OK`, `mvn package OK` (Java 23 com `release 21`)

### Próximos passos (P1-P3)

- [ ] Ativar branch protection em `Settings > Branches` (1 min, conforme `.github/BRANCH_PROTECTION.md:5`)
- [ ] Slides 8-10min (roteiro sugerido §7)
- [ ] (Opcional) `maven-git-commit-id-plugin` para expor SHA em `GET /status`

---

## 7. Roteiro de Apresentação (8-10min) — Entrega 5

1. **Cenário (1min):** 7 dores `README.md:11-17`, pipeline inicial `ci.yaml:22-31` com `-DskipTests`.
2. **Decisões (3min):** Ordem por custo, bloqueio `needs`, unit vs IT paralelos (`pom.xml:52-80`), artefato só após verde, locais proibidos.
3. **YAML (2min):** Mostrar `ci.yaml:4-7,15-17,39,65,91,105,118,123` — o que mudou e por que.
4. **Governança (1min):** Branch protection `BRANCH_PROTECTION.md:13-19` — PR bloqueado com pipeline vermelha.
5. **Tempo (1min):** 22min → ~7min com cache + paralelismo, sem remover testes.
6. **Rastreabilidade (1min):** `api-<sha>.jar` + `commit.txt` + provenance → `POST /releases/validate`.
7. **Fecho (30s):** “Não basta dizer o que fariam, precisa explicar por que” `README.md:45` — cada decisão tem trade-off documentado acima.

---

## 8. Como validar

```powershell
# Local (reproduz a CI)
$env:JAVA_HOME="C:\Program Files\Java\jdk-23"
.\mvnw.cmd clean compile
.\mvnw.cmd test        # Surefire *Test.java
.\mvnw.cmd verify      # Failsafe *IT.java
.\mvnw.cmd package     # só após testes verdes
Get-ChildItem target/*.jar

# CI
# Push em branch -> PR para main -> verificar 4 checks verdes + artefato api-<sha>.jar
# Tentar merge com teste quebrado -> botão Merge bloqueado
```
