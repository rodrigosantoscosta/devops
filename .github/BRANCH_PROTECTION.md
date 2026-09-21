# Branch Protection - main (P0)

Este arquivo documenta as regras obrigatórias para corrigir `README.md:15` - "Pull Requests conseguem ser integrados mesmo quando a pipeline falha".

## Como ativar no GitHub

`Settings > Branches > Add branch protection rule`

- **Branch name pattern:** `main`
- [x] **Require a pull request before merging**
  - [x] Require approvals: `1`
  - [x] Dismiss stale pull request approvals when new commits are pushed
- [x] **Require status checks to pass before merging**
  - [x] Require branches to be up to date before merging
  - **Status checks obrigatórios (P3 split PR vs main):**
    - **Em Pull Request:** `Compile`, `Unit Tests` (feedback ~2-3min; `Integration Tests` e `Package` skippados via `if: push` em `ci.yaml:67,95`)
    - **Em push para `main`:** `Compile`, `Unit Tests`, `Integration Tests` (`ci.yaml:67`), `Package & Publish` (`ci.yaml:95`) — todos obrigatórios; `Package` só em `push main` com `needs: [unit,integration]` (`:96`)
- [x] **Do not allow bypassing the above settings**
- [ ] Allow force pushes: `desmarcado`
- [ ] Allow deletions: `desmarcado`

## Efeito

- PR não pode ser mergeado com pipeline vermelha.
- `Compile` falhou -> `Unit Tests`/`Integration Tests`/`Package` nem executam (`needs: compile`).
- Teste falhou -> `Package & Publish` não executa (depende de `needs: [unit-tests, integration-tests]`), logo nenhum JAR publicável é gerado. Em caso de falha apenas `surefire-reports`/`failsafe-reports` são publicados para diagnóstico com `if: failure()` e `retention-days: 3`.
- **P3:** PR valida `compile + unit` (~2-3min, `mvn test 13 OK`) e libera merge rápido; `push` para `main` valida completo `verify 2 IT OK` + `package` + `provenance` (~7-8min). Reduz 22min → PR rápido sem perder cobertura em `main`.
- Artefatos locais nunca são aceitos: deploy deve usar `api-${{ github.sha }}` baixado via `gh run download` ou Release, validando `target/commit.txt`.

## Validação

Após ativar, teste:
1. Abra PR com teste quebrado -> verifique que botão `Merge` fica bloqueado.
2. Push direto em `main` deve ser rejeitado (apenas PR).

## Auditoria

Provenance gerado por `actions/attest-build-provenance@v2` (apenas em `main`) garante que `api-<sha>.jar` veio da execução `run_id` registrada em `target/commit.txt`.
