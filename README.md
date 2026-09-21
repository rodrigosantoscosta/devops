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
