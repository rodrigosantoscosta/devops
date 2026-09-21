# Ordem de Prioridades — Comitê de CI

> Priorização por Impacto / Dependência / Risco se atrasar. Base: `README.md:9-39` e `PROPOSTA.md:5-6`.
> Objetivo: decidir o que entregar primeiro sem retrabalho.

## Resumo Executivo

```
P0 (bloqueante) -> P1 (essencial para nota) -> P2 (diferencial) -> P3 (otimização 22min) -> P4 (slides)
```

Se tempo for curto, entregue na ordem `README.md:35-39` que o avaliador corrige: `3.YAML > 2.Regras > 1.Diagrama > 4.Justificativa`.

---

## P0 — Bloqueante (fazer HOJE, 30-45min, resolve 80% dos problemas `README.md:11-17`)

| # | Entrega | Por que primeiro | Esforço | Se atrasar |
|---|---|---|---|---|
| **1** | **YAML `.github/workflows/ci.yaml:1` corrigido** — Entrega 3 `README.md:38` | Sem isso todo resto é teoria. Corrige `compile -> test -> verify -> package` com `cache: maven` e sem `-DskipTests` e `if: success()` no `upload-artifact`. Bloqueia JAR com teste falho `README.md:13,99`. | Baixo — 1 arquivo, 4 jobs com `needs` | Continua publicando JAR quebrado, PRs vermelhos passam |
| **2** | **Branch Protection em `main`** — parte da Entrega 2 `README.md:37` | Impede `README.md:15,101` (merge com pipeline vermelha) mesmo que YAML esteja certo. Sem isso YAML é ignorável. Depende de **1**. | Baixo — 3 cliques `Settings > Branches` + `.github/BRANCH_PROTECTION.md:1` | Regra existe só no papel |

**Efeito ao concluir P0:** elimina `1,3,5` da lista de problemas e responde `quando artefato pode ser gerado` e `quais bloqueiam` (`README.md:24-25`). Validado local: `mvn test 13 OK`, `mvn verify 2 IT OK` (commit `124637b`).

## P1 — Essencial para Nota e Governança (fazer em seguida, ~1h)

| # | Entrega | Por que | Depende de | Arquivo |
|---|---|---|---|---|
| **3** | **Regras do Processo** — Entrega 2 `README.md:37` | Documento que o professor cobra o “por que” `README.md:45`. Formaliza: proibição de artefato local `README.md:14,28`, `fail-fast`, retenção, rastreabilidade `commit SHA` `README.md:16,29`. Valida decisões do P0. | P0 | `PROPOSTA.md:60-122` + `BRANCH_PROTECTION.md` |
| **4** | **Diagrama da Pipeline** — Entrega 1 `README.md:36` | Avaliativo obrigatório. Só faz sentido depois que YAML está decidido — sem YAML, diagrama é chute. | P0 | `PROPOSTA.md:21-56` (Mermaid) |

## P2 — Justificativa (diferencial de nota)

| # | Entrega | Por que | Depende de |
|---|---|---|---|
| **5** | **Justificativa Curta** — Entrega 4 `README.md:39` + `> Não basta dizer o que fariam. O grupo precisa explicar por que. README.md:45` | Onde ganha nota máxima. Responde `testes juntos/separados`, `artefato local`, `relacionar artefato ao commit`, `reduzir tempo sem remover teste` `README.md:26-31`. Sem P0/P1 não há o que justificar. | P0+P1 | `PROPOSTA.md:148-159` |

## P3 — Otimização 22min (Desafio Extra `README.md:107-113`)

| # | Entrega | Por que por último | Depende de |
|---|---|---|---|
| **6** | **Cache + Paralelismo `unit` vs `IT` + split `PR` vs `main`** | Só entra após pipeline correta. Otimizar pipeline errada (`package -DskipTests` antigo `:25`) só deixa erro mais rápido. `pom.xml:52-80` já separa `surefire (*Test)` e `failsafe (*IT)` → explorar paralelismo economiza ~60% sem perder cobertura. | P0 |

Técnicas já embutidas no P0: `cache: maven` `:32,50,76,102`, jobs paralelos `needs: compile` `:39,65`, `concurrency.cancel-in-progress` `:15-17`.

## P4 — Apresentação (final)

| # | Entrega | Por que por último | Depende de |
|---|---|---|---|
| **7** | **Slides 8-10min** — Entrega 5 `README.md:40` | Consolida 1-6. Depende de tudo. | Todos | `PROPOSTA.md:187-195` roteiro |

---

## Sequência Recomendada de Execução

```
1. YAML (P0) -> 2. Branch Protection (P0) -> 3. Teste local .\mvnw.cmd verify
      -> 4. Regras (P1) -> 5. Diagrama Mermaid (P1) -> 6. Justificativa (P2)
      -> 7. Otimização cache/paralelo (P3) -> 8. Slides (P4)
```

**Status atual (após validação P2 2026-09-21):** P0 ✅ `124637b`, P1 ✅ `PROPOSTA.md:21` + `PROPOSTA.md:60` + `BRANCH_PROTECTION.md:13`, P2 ✅ `PROPOSTA.md:149` (5 justificativas com trade-off) — validado, P3 ✅ `ci.yaml:15-17,32,39,65`, P4 🔜 pendente (roteiro `PROPOSTA.md:187`)

## Checklist de Decisão

- [x] P0 YAML bloqueante — `.github/workflows/ci.yaml:1`
- [x] P0 Branch Protection — `.github/BRANCH_PROTECTION.md:1` (falta ativar no GitHub)
- [x] P1 Regras — `PROPOSTA.md:60` (8 regras: ordem, bloqueio, artefato, unit vs IT `pom.xml:52-80`, checks obrigatórios `BRANCH_PROTECTION.md:13`, locais proibidos, rastreabilidade `ci.yaml:118`, tempo `ci.yaml:32`) — validado 2026-09-21
- [x] P1 Diagrama — `PROPOSTA.md:21` (Mermaid `compile -> unit || integration -> package` + variante 22min `PROPOSTA.md:47`) — validado 2026-09-21
- [x] P2 Justificativa — `PROPOSTA.md:149` (5 porquês com alternativa rejeitada + trade-off: JAR falho `ci.yaml:105` vs reports `ci.yaml:56`, IT `pom.xml:64` vs PR `ci.yaml:4`, PR vermelha `BRANCH_PROTECTION.md:13`, api-sha `ci.yaml:118`, velocidade `ci.yaml:32` — validado 2026-09-21)
- [x] P3 Cache/paralelismo — já em `ci.yaml:15-17,32,39,65`
- [ ] P4 Slides — usar roteiro `PROPOSTA.md:187`
