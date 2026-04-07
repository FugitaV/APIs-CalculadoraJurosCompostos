# Simulador de Juros Compostos — Documentação Técnica

Sistema serverless de cálculo de juros compostos com suporte a intervalos de aporte variáveis, construído sobre AWS Lambda + API Gateway + DynamoDB.

---

## Índice

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Serviços AWS](#serviços-aws)
- [Backend — Lambda Java](#backend--lambda-java)
- [Frontend](#frontend)
- [API Reference](#api-reference)
- [Modelo de Dados — DynamoDB](#modelo-de-dados--dynamodb)
- [Fórmulas Financeiras](#fórmulas-financeiras)
- [Build e Deploy](#build-e-deploy)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Testes](#testes)
- [Configurações de Segurança e Limites](#configurações-de-segurança-e-limites)

---

## Visão Geral

O Simulador de Juros Compostos permite calcular projeções de investimento considerando múltiplos intervalos de aporte. Cada intervalo pode ter um valor mensal diferente, uma duração em meses e um aporte extra injetado no início do período — o que possibilita simular cenários reais como aumento de salário, recebimento de bônus ou períodos sem contribuição.

```
Usuário → Frontend → API Gateway → Lambda → DynamoDB
```

---

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                        AWS Cloud (sa-east-1)                    │
│                                                                 │
│   ┌──────────────────┐        ┌──────────────────────────────┐  │
│   │   API Gateway    │        │     Lambda Function          │  │
│   │  HTTP API        │──POST─▶│  calculadora-juros-compostos │  │
│   │  /prod/calcular  │        │  Runtime: Java 21            │  │
│   └──────────────────┘        │  Framework: Spring Cloud Fn  │  │
│           ▲                   └──────────────┬───────────────┘  │
│           │                                  │ PutItem          │
│    X-User-Id header                          ▼                  │
│                               ┌──────────────────────────────┐  │
│                               │         DynamoDB             │  │
│                               │       Table: simulations     │  │
│                               └──────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
          ▲
          │ HTTPS
┌─────────────────────┐
│      Frontend       │
│   (documentado      │
│    na seção abaixo) │
└─────────────────────┘
```

### Arquitetura Interna da Lambda — Hexagonal (Ports & Adapters)

```
adapter.in          application              domain
────────────        ────────────────         ──────────────────────
CalculationFn  ───▶ CalculateInterest  ───▶  CompoundInterestService
(Function<         UseCase (port)             │
 Message<Req>,                                ├─▶ SimpleIntervalStrategy
 Response>)    ───▶ SaveSimulation     ◀───   ├─▶ ExtraContributionStrategy
                    Port (port)               └─▶ ZeroContributionStrategy
adapter.out
────────────
DynamoSimulation
Repository
```

---

## Serviços AWS

### API Gateway — HTTP API

| Propriedade | Valor |
|---|---|
| Tipo | HTTP API |
| Endpoint | `https://13zv2pr424.execute-api.sa-east-1.amazonaws.com/prod` |
| Rota | `POST /calcular` |
| Integração | Lambda Proxy |
| Stage | `prod` |
| Rate limit | 100 req/s (burst: 200) |
| Throttling por rota | 50 req/s (burst: 100) |
| CORS | Habilitado (`*`) |

O API Gateway recebe a requisição do frontend, injeta os headers originais (incluindo `X-User-Id`) e repassa para a Lambda no formato API Gateway Proxy v1. A resposta é devolvida diretamente ao cliente.

---

### Lambda

| Propriedade | Valor |
|---|---|
| Nome | `calculadora-juros-compostos` |
| Runtime | Java 21 |
| Handler | `org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest` |
| Memória | 512 MB |
| Timeout | 30 segundos |
| SnapStart | Recomendado (reduz cold start) |

A Lambda usa **Spring Cloud Function** como framework, o que permite desenvolver a lógica como um simples `Function<Input, Output>` sem depender de nenhuma interface do AWS SDK. O `FunctionInvoker` faz a ponte entre o evento do API Gateway e o Spring.

---

### DynamoDB

| Propriedade | Valor |
|---|---|
| Tabela | `simulations` |
| Partition Key (PK) | `pk` → `userId#simulationId` |
| Sort Key (SK) | `sk` → timestamp ISO-8601 |
| TTL | Campo `ttl` (Unix epoch) — expira em 90 dias |
| GSI | `userId-createdAt-index` (listagem por usuário) |
| Billing | On-demand (pay per request) |

---

## Backend — Lambda Java

### Stack

| Tecnologia | Versão | Função |
|---|---|---|
| Java | 21 (LTS) | Runtime |
| Spring Boot | 3.4.4 | Framework base |
| Spring Cloud Function | 4.x | Abstração de Function para Lambda |
| spring-cloud-function-adapter-aws | 4.x | Bridge Lambda ↔ Spring |
| AWS SDK v2 — DynamoDB Enhanced | 2.29.0 | Persistência |
| Lombok | 1.18.38 | Redução de boilerplate |
| Jackson | 2.x | Serialização JSON |
| JUnit 5 + Mockito | — | Testes unitários |
| Maven Shade Plugin | 3.5.1 | Fat JAR para deploy |

### Estrutura de Pacotes

```
com.example.calculadora
├── CalculadoraApplication.java          # Entry point Spring Boot
├── adapter
│   ├── in
│   │   ├── CalculationFunction.java     # Function<Message<Req>, Res>
│   │   ├── CalculationRequest.java      # Payload de entrada
│   │   └── CalculationResponse.java     # Payload de saída
│   └── out
│       └── DynamoSimulationRepository.java  # Implementa SaveSimulationPort
├── application
│   ├── port
│   │   ├── CalculateInterestUseCase.java
│   │   └── SaveSimulationPort.java
│   └── service
│       └── CompoundInterestService.java # Orquestra cálculo + persistência
├── config
│   └── FunctionConfig.java              # Registra beans Spring
└── domain
    ├── model
    │   ├── Simulation.java              # Entidade DynamoDB
    │   ├── SimulationInterval.java      # Intervalo de aporte
    │   ├── SimulationResult.java        # Resultado completo
    │   └── IntervalResult.java          # Resultado por intervalo
    └── strategy
        ├── IntervalStrategy.java            # Interface Strategy
        ├── SimpleIntervalStrategy.java      # Aporte mensal fixo
        ├── ExtraContributionStrategy.java   # Aporte mensal + extra
        └── ZeroContributionStrategy.java    # Sem aporte (pausa)
```

### Padrão Strategy — Resolução de Estratégia

O `CompoundInterestService` escolhe automaticamente a estratégia correta para cada intervalo:

```
extraContribution > 0  →  ExtraContributionStrategy
monthlyContribution > 0  →  SimpleIntervalStrategy
caso contrário  →  ZeroContributionStrategy
```

---

## Frontend

> 📝 **Seção reservada para documentação do frontend**
>
> Documente aqui:
> - Stack utilizada (React, Vue, etc.)
> - Como configurar e rodar localmente
> - Como apontar para o endpoint da API
> - Variáveis de ambiente necessárias
> - Como fazer build para produção
> - URL de produção

---

## API Reference

### `POST /prod/calcular`

Calcula a projeção de juros compostos para uma série de intervalos de aporte.

**URL**
```
https://13zv2pr424.execute-api.sa-east-1.amazonaws.com/prod/calcular
```

**Headers**

| Header | Obrigatório | Descrição |
|---|---|---|
| `Content-Type` | Sim | `application/json` |
| `X-User-Id` | Não | Identificador do usuário para associar a simulação. Default: `anonymous` |

**Request Body**

```json
{
  "initialValue": 10000.00,
  "annualRate": 12.00,
  "intervals": [
    {
      "monthlyContribution": 500.00,
      "periodInMonths": 12,
      "extraContribution": null
    },
    {
      "monthlyContribution": 1000.00,
      "periodInMonths": 24,
      "extraContribution": 10000.00
    }
  ]
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `initialValue` | `number` | Valor inicial investido (R$) |
| `annualRate` | `number` | Taxa de juros anual em % (ex: `12.00` = 12% a.a.) |
| `intervals` | `array` | Lista de intervalos de investimento |
| `intervals[].monthlyContribution` | `number` | Aporte mensal do intervalo (R$) |
| `intervals[].periodInMonths` | `integer` | Duração do intervalo em meses |
| `intervals[].extraContribution` | `number\|null` | Aporte extra injetado no início do intervalo (R$) |

**Response — 200 OK**

```json
{
  "totalInvested": 16000.00,
  "totalInterest": 1523.25,
  "finalValue": 17523.25,
  "intervals": [
    {
      "finalBalance": 17523.25,
      "totalContributed": 6000.00,
      "interestEarned": 1523.25
    }
  ]
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `totalInvested` | `number` | Valor inicial + soma de todos os aportes |
| `totalInterest` | `number` | Total de juros gerados |
| `finalValue` | `number` | Saldo final = totalInvested + totalInterest |
| `intervals[].finalBalance` | `number` | Saldo ao final do intervalo |
| `intervals[].totalContributed` | `number` | Total aportado no intervalo (mensal × meses + extra) |
| `intervals[].interestEarned` | `number` | Juros gerados no intervalo |

**Exemplo com curl**

```bash
curl -X POST https://13zv2pr424.execute-api.sa-east-1.amazonaws.com/prod/calcular \
  -H "Content-Type: application/json" \
  -H "X-User-Id: usuario123" \
  -d '{
    "initialValue": 10000.00,
    "annualRate": 12.00,
    "intervals": [
      { "monthlyContribution": 500.00, "periodInMonths": 12, "extraContribution": null }
    ]
  }'
```

---

## Modelo de Dados — DynamoDB

### Tabela `simulations`

| Atributo | Tipo | Descrição |
|---|---|---|
| `pk` | String | Chave primária: `userId#simulationId` |
| `sk` | String | Sort key: timestamp ISO-8601 da criação |
| `userId` | String | Identificador do usuário (GSI partition key) |
| `simulation_id` | String | UUID gerado pela aplicação |
| `ttl` | Number | Unix epoch de expiração (90 dias após criação) |
| `initialValue` | Number | Valor inicial da simulação |
| `annualRate` | Number | Taxa anual informada |
| `totalInvested` | Number | Total investido calculado |
| `totalInterest` | Number | Total de juros calculado |
| `finalValue` | Number | Saldo final calculado |

### GSI — `userId-createdAt-index`

Permite listar todas as simulações de um usuário ordenadas por data:

| | Chave |
|---|---|
| Partition Key | `userId` |
| Sort Key | `sk` (createdAt) |

---

## Fórmulas Financeiras

### Conversão de Taxa

A taxa anual informada é convertida para taxa mensal equivalente usando a fórmula de **taxa efetiva**:

```
r_mensal = (1 + r_anual) ^ (1/12) - 1

Exemplo: 12% a.a. → (1,12)^(1/12) - 1 ≈ 0,9489% a.m.
```

### SimpleIntervalStrategy — Aporte mensal com juros compostos

```
FV = PV × (1 + r)^n  +  PMT × ((1 + r)^n - 1) / r

Onde:
  PV  = saldo inicial do intervalo
  PMT = aporte mensal
  r   = taxa mensal decimal
  n   = número de meses
```

### ExtraContributionStrategy — Aporte extra no início

```
saldo_ajustado = PV + extraContribution
FV = aplicar SimpleIntervalStrategy com saldo_ajustado
totalContributed = PMT × n + extraContribution
```

### ZeroContributionStrategy — Sem aporte (pausa)

```
FV = PV × (1 + r)^n
```

---

## Build e Deploy

### Pré-requisitos

- JDK 21+
- Maven 3.8+
- Conta AWS com permissões em Lambda, DynamoDB e API Gateway

### Build

```bash
mvn clean package -DskipTests
```

Gera: `target/calculadora-juros-compostos-1.0.0.jar`

### Deploy Manual (Console AWS)

1. Lambda → sua função → **Code → Upload from → .zip or .jar file**
2. Selecione `target/calculadora-juros-compostos-1.0.0.jar`
3. Clique em **Save**

### Deploy via CLI

```bash
aws lambda update-function-code \
  --function-name calculadora-juros-compostos \
  --zip-file fileb://target/calculadora-juros-compostos-1.0.0.jar \
  --region sa-east-1
```

---

## Variáveis de Ambiente

Configuradas na Lambda via **Configuration → Environment variables**:

| Variável | Valor | Descrição |
|---|---|---|
| `MAIN_CLASS` | `com.example.calculadora.CalculadoraApplication` | Classe principal para o FunctionInvoker |
| `DYNAMODB_TABLE_NAME` | `simulations` | Nome da tabela DynamoDB |
| `AWS_REGION` | `sa-east-1` | Região dos serviços AWS |

---

## Testes

### Unitários

```bash
mvn test
```

| Teste | O que valida |
|---|---|
| `SimpleIntervalStrategyTest` | Fórmula FV com taxa 1% a.m., 12 meses, PMT 500 |
| `ExtraContributionStrategyTest` | Aporte extra somado ao saldo antes do cálculo |
| `CompoundInterestServiceTest` | Totais e mock do repositório com 2 intervalos |
| `LambdaGatewaySimulationTest` | Simulação completa da requisição do API Gateway |

### Simulação local da Lambda

```bash
mvn test -Dtest=LambdaGatewaySimulationTest
```

Instancia toda a cadeia (sem Spring context, sem conexão AWS) e imprime o JSON de resposta no console.

---

## Configurações de Segurança e Limites

### Rate Limiting — API Gateway

| Nível | Rate | Burst |
|---|---|---|
| API global | 100 req/s | 200 |
| `POST /calcular` | 50 req/s | 100 |

### Budget Alert — AWS Billing

Alerta configurado em **AWS Budgets** para notificar por email ao atingir 85% e 100% do limite mensal definido.

### IAM — Permissões da Lambda

A role `calculadora-juros-compostos-role-icl7w57r` possui policy com escopo mínimo:

```json
{
  "Effect": "Allow",
  "Action": ["dynamodb:PutItem", "dynamodb:Query"],
  "Resource": [
    "arn:aws:dynamodb:sa-east-1:*:table/simulations",
    "arn:aws:dynamodb:sa-east-1:*:table/simulations/index/*"
  ]
}
```

### TTL — Dados expiram automaticamente

Simulações são removidas automaticamente do DynamoDB após **90 dias** via TTL nativo, sem custo adicional de operação.
