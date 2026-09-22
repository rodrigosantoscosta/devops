# Atividade: Comite de CI

Este repositorio e o projeto base para uma simulacao de decisao de engenharia em DevOps.

O Grupo deve assumir o papel de um pequeno comite responsavel por redesenhar o processo de integracao continua de uma API Java 21 usada por clientes reais.

## Cenario

A equipe mantem uma API Java 21 com Maven e utiliza uma pipeline simples no GitHub Actions. Com o crescimento do projeto, comecaram a ocorrer os seguintes problemas:

- Alteracoes que nao compilam chegam aos Pull Requests e, algumas vezes, a branch principal.
- A quantidade de testes unitarios e de integracao aumentou, elevando o tempo da pipeline e atrasando o feedback aos desenvolvedores.
- Quando um teste falha, alguns integrantes defendem que o JAR seja gerado e liberado mesmo assim.
- Artefatos gerados localmente estao sendo enviados ao servidor, sem garantia da versao do Java, das dependencias ou dos testes executados.
- Pull Requests conseguem ser integrados a branch principal mesmo quando a pipeline falha.
- A equipe nao consegue relacionar facilmente um artefato implantado ao commit e a execucao da pipeline que o produziram.
- A pipeline leva aproximadamente 22 minutos, mas a equipe nao quer ganhar velocidade removendo verificacoes importantes.

## Missao do grupo

O grupo deve redesenhar o processo de CI da aplicacao e decidir:

- quais etapas a pipeline deve possuir;
- em que ordem essas etapas serao executadas;
- quais etapas devem bloquear o fluxo em caso de falha;
- quando o artefato pode ser gerado e quando ele pode ser considerado publicavel;
- se testes unitarios e testes de integracao devem executar juntos ou em momentos diferentes;
- quais verificacoes devem ser obrigatorias antes de um merge;
- se artefatos gerados localmente podem ser aceitos;
- como relacionar um artefato ao commit que o originou;
- como reduzir o tempo da pipeline sem remover verificacoes importantes.

## Entrega

Cada grupo deve apresentar:

1. Um diagrama da pipeline proposta.
2. As regras do processo.
3. Um YAML da pipeline.
4. Uma justificativa curta para as decisoes mais importantes.
5. Uma apresentacao oral de 8 a 10 minutos.

A regra mais importante:

> Nao basta dizer o que fariam. O grupo precisa explicar por que.

## Projeto

Este projeto e uma API Spring Boot simples com Java 21 e Maven.

Endpoints disponiveis:

```text
GET /status
POST /releases/validate
```

Exemplo de validacao de uma release:

```json
{
  "commit": "abc1234",
  "javaVersion": 21,
  "unitTestsPassed": true,
  "integrationTestsPassed": false
}
```

Comandos uteis:

```bash
./mvnw clean compile
./mvnw test
./mvnw verify
./mvnw package -DskipTests
```

No Windows PowerShell:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd package -DskipTests
```

Os testes unitarios ficam em `src/test/java/com/devops/api` e verificam as regras de validacao de uma release, a mensagem de status, a resposta do controller, o formato do timestamp e o comportamento dos records. Eles devem ser executados rapidamente durante o desenvolvimento.

Os testes com sufixo `IT` sobem a aplicacao e chamam a API de verdade. Eles representam verificacoes de integracao e por isso sao executados na fase `verify`.

## Ponto de partida

A pipeline inicial esta em `.github/workflows/ci.yaml`.

Ela compila a aplicacao, gera o JAR sem executar os testes e publica o artefato no GitHub Actions.

Ela e propositalmente simples para provocar a discussao:

- Ela deve gerar o JAR se os testes falharem?
- Testes de integracao devem rodar em todo push?
- Um PR pode ser integrado a branch principal com a pipeline vermelha?
- Um JAR gerado apos uma falha pode ser armazenado apenas para diagnostico?
- Como garantir que o JAR publicado foi exatamente o que passou pela pipeline?
- O nome do artefato deve incluir o commit?
- Pipeline rapida e mais importante que pipeline completa?

## Desafio extra

Depois da primeira proposta, considere esta nova informacao:

> A pipeline esta demorando 22 minutos e os desenvolvedores precisam de feedback mais rapido.

Redesenhe o fluxo para reduzir o tempo sem simplesmente remover testes importantes. Considere paralelismo, cache e a separacao das verificacoes executadas em Pull Requests e na branch principal.

---

## Resolucao da Atividade

Entregas do Comite de CI (detalhes em `PROPOSTA.md`, regras em `.github/BRANCH_PROTECTION.md`):

| # | Entrega | Status | Arquivo |
|---|---|---|---|
| 1 | Diagrama da pipeline | ✅ | `PROPOSTA.md` §2 (Mermaid) |
| 2 | Regras do processo | ✅ | `PROPOSTA.md` §3 + `.github/BRANCH_PROTECTION.md` |
| 3 | YAML da pipeline | ✅ | `.github/workflows/ci.yaml` |
| 4 | Justificativa | ✅ | `PROPOSTA.md` §5 |
| 5 | Apresentacao 8-10min | Pendente | roteiro em `PROPOSTA.md` §7 |

### Pipeline implementada

`ci.yaml` com 4 jobs bloqueantes: `compile → unit-tests || integration-tests → package` (`needs` + `cache: maven` + `concurrency`).

- **PR:** `Compile` + `Unit Tests` (~2-3min) — Integration/Package skippados via `if: push`
- **Push main/master/devs:** completo `verify` + `package` + `provenance` (~7-8min, era 22min)
- **Branch Protection** ativa em `master`: PR obrigatório (1 approval), checks `Compile` + `Unit Tests`, sem force-push

### Ciclo verde → vermelho → verde (demo de CI)

| Commit | Branch | Mudanca | Resultado Actions |
|---|---|---|---|
| `819a531` | demo | quebra `StatusServiceTest` | ❌ Unit Tests falha, Package nao roda |
| `b5d4323` | demo | corrige teste | ✅ volta verde |
| `a54c70d` | devs | `CIPipelineCasesTest` +14 casos | ✅ |
| `4561a66` | devs | `CIPipelineEdgeCasesTest` +12 casos | ✅ |
| `24d8b50` | devs | inverte expectativa edge case | ❌ Unit Tests falha |
| `ee86073` | master | quebra `StatusServiceTest` | ❌ Unit Tests falha |
| `8680824` | master | restaura teste | ✅ |
| `b324701` | devs | restaura 2 testes | ✅ |
| `5e6e0f4` | devs | repara `ci.yaml` trigger (merge removeu `devs`) | ✅ |

## Tabela de Casos de Teste de CI

**51 testes = 45 unit (Surefire `*Test.java`) + 6 integration (Failsafe `*IT.java`)** — branch `devs`.

### Cobertura por arquivo

| # | Arquivo | Tipo | Qtd | Commit | Casos cobertos |
|---|---|---|---|---|---|
| 1 | `StatusServiceTest` | unit | 1 | `124637b` | Mensagem de status = `"API Java 21 pronta para CI"` (porta de compile) |
| 2 | `StatusResponseTest` | unit | 2 | `124637b` | Record `StatusResponse` — dados preservados, igualdade/hash |
| 3 | `StatusControllerTest` | unit | 3 | `124637b` | Delegacao ao service, timestamp ISO8601, timestamp proximo ao agora |
| 4 | `StatusControllerIT` | IT | 1 | `124637b` | `GET /status` → 200 + body com mensagem e `generatedAt` |
| 5 | `ReleaseValidationServiceTest` | unit | 6 | `124637b` | Aprova valida; rejeita sem commit, Java≠21, unit falho, IT falho; 4 motivos juntos |
| 6 | `ReleaseValidationRequestTest` | unit | 3 | `cea1fc2` | Record request — preservar, comparar, commit nulo aceito |
| 7 | `ReleaseValidationResponseTest` | unit | 3 | `cea1fc2` | Record response — preservar, comparar, motivos preservados |
| 8 | `ReleaseValidationControllerIT` | IT | 5 | `cea1fc2` | `POST /releases/validate`: happy + rejeita IT falho, commit nulo, Java≠21, unit falho |
| 9 | **`CIPipelineCasesTest`** | unit | **14** | `a54c70d` | Cenarios de pipeline (abaixo) |
| 10 | **`CIPipelineEdgeCasesTest`** | unit | **12** | `4561a66` | Edge cases (abaixo) |

### `CIPipelineCasesTest` — 14 casos (`a54c70d`)

| Nested / Caso | Teste | Qtd | O que valida na CI |
|---|---|---|---|
| Caso 1: compile | `deveRetornarMensagemCompilavel` | 1 | Codigo compila e retorna mensagem esperada |
| | `mensagemNaoDeveSerVazia` | 1 | Resposta nunca em branco |
| Caso 2: pipeline | `pipelineVerde_quandoTudoAprovado` | 1 | Tudo verde → `approved=true`, `reasons=[]` |
| | `pipelineVermelha_quandoUnitFalha` | 1 | `unitTestsPassed=false` → rejeita |
| | `pipelineVermelha_quandoIntegrationFalha` | 1 | `integrationTestsPassed=false` → rejeita |
| | `pipelineVermelha_quandoCommitAusente` | 1 | Commit blank → rejeita |
| | `pipelineVermelha_quandoJavaVersionDiferenteDe21` | 5 | Java 8,11,17,22,23 → rejeita (`@ValueSource`) |
| | `pipelineVermelha_quandoTodosFalham` | 1 | Todos falham → 4 motivos |
| Caso 3: rastreabilidade | `commitNuloDeveSerRejeitado` | 1 | Commit null → artefato sem SHA nao passa |
| | `commitValidoDeveSerAceito` | 1 | Commit valido → aprovado |

### `CIPipelineEdgeCasesTest` — 12 casos (`4561a66`)

| Nested / Grupo | Teste | Qtd | O que valida na CI |
|---|---|---|---|
| Commit edge | `commitVazioOuBlankDeveSerRejeitado` | 6 | `null`, `""`, `" "`, `"  "`, `"\t"`, `"\n"` → rejeita (`@NullAndEmptySource` + `@ValueSource`) |
| | `commitComEspacosNasPontasDeveSerAceito` | 1 | `" abc1234 "` aceito (sem trim; `isBlank` so rejeita blank puro) |
| Imutabilidade | `reasonsDeveSerImutavel` | 1 | `List.copyOf` → `UnsupportedOperationException` em lista cheia |
| | `reasonsVazioDeveSerImutavel` | 1 | Mesmo para lista vazia |
| Contrato `/status` | `statusResponseDeveTerStatusEGeneratedAtNaoNulos` | 1 | Campos nao nulos |
| | `generatedAtDeveSerISO8601Parseavel` | 1 | `Instant.parse` nao lanca |
| | `reasonsOrdenadosComoNoCodigo` | 1 | Ordem exata das 4 mensagens de rejeicao |

### Mapeamento problema → teste

| Problema do cenario (`README.md:11-17`) | Teste que cobre |
|---|---|
| Alteracoes nao compilam chegam a PR/`main` | `StatusServiceTest`, `CIPipelineCasesTest` Caso 1 |
| Pipeline lenta / feedback tardio | split PR vs main em `ci.yaml` + suite rapida unit (45 em ~2s local) |
| JAR gerado com teste falho | `CIPipelineCasesTest` Caso 2 (unit/IT falho → rejeita); package so com `needs` verdes |
| Artefato local sem garantia | `POST /releases/validate` valida `javaVersion`+flags; `CIPipelineCasesTest` Caso 2 |
| PR merge com pipeline vermelha | Branch Protection `Compile`+`Unit Tests`; demo `819a531`/`ee86073` |
| Sem rastreabilidade artefato→commit | `CIPipelineCasesTest` Caso 3 + edge commit; `api-${sha}` + `commit.txt` |
| 22min sem cortar verificacoes | cache+paralelismo+split em `ci.yaml` (22min → PR ~2-3 / push ~7-8) |

### Validacao

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
.\mvnw.cmd -B test      # 45 OK (devs) / 19 OK (master)
.\mvnw.cmd -B verify    # 51 OK (45 unit + 6 IT)
```
