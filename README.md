# Simulador de Juros Compostos — Documentação Técnica

Sistema serverless de cálculo de juros compostos com suporte a intervalos de aporte variáveis, exportação de relatório em Excel e persistência de histórico. Construído sobre AWS Lambda + API Gateway + DynamoDB.
http://calculadora-juros-compostos-frontend.s3-website-sa-east-1.amazonaws.com/
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

O Simulador de Juros Compostos permite calcular projeções de investimento considerando múltiplos intervalos de aporte. Cada intervalo pode ter um valor mensal diferente, uma duração em meses e um aporte extra injetado no início do período — possibilitando simular cenários reais como aumento de salário, recebimento de bônus ou períodos sem contribuição.

Após a simulação, o usuário pode exportar um relatório `.xlsx` completo com parâmetros, aportes e evolução mês a mês.

```
Usuário → Frontend (S3) → API Gateway → Lambda (cálculo)  → DynamoDB
                                      → Lambda (exportação) ↗
```

---

## Arquitetura



### Arquitetura Interna — Hexagonal (Ports & Adapters)

```
adapter.in                 application                   domain
──────────────             ──────────────────            ──────────────────────
CalculationFn  ──────────▶ CalculateInterest  ─────────▶ CompoundInterestService
(Function<                 UseCase (port)                 │
 Message<Req>, Res>)                                      ├─▶ SimpleIntervalStrategy
                           SaveSimulation     ◀────────   ├─▶ ExtraContributionStrategy
ExportFn       ──────────▶ Port (port)                    └─▶ ZeroContributionStrategy
(Function<
 Message<Req>, Res>)       ExportSimulation   ─────────▶ ExcelExportService
                           UseCase (port)                 (Apache POI)

                           GetSimulation      ◀────────
                           Port (port)

adapter.out
────────────
DynamoSimulationRepository   (implementa SaveSimulationPort + GetSimulationPort)
```

<img width="1087" height="426" alt="arquitetura-calculadora-juros-compostos" src="https://github.com/user-attachments/assets/c1840c7b-6385-48b0-9ea6-de6939231513" />

---

## Serviços AWS

### API Gateway — HTTP API

| Propriedade | Valor |
|---|---|
| Tipo | HTTP API |
| Endpoint base | `https://13zv2pr424.execute-api.sa-east-1.amazonaws.com/prod` |
| Rota — cálculo | `POST /calcular` |
| Rota — exportação | `POST /exportar` |
| Integração | Lambda Proxy |
| Stage | `prod` |
| Rate limit global | 100 req/s (burst: 200) |
| Throttling por rota | 50 req/s (burst: 100) |
| CORS | Habilitado (`*`) |

---

### Lambdas

O sistema usa **duas funções Lambda distintas** implantadas a partir do **mesmo JAR**. A separação é feita pela variável de ambiente `SPRING_CLOUD_FUNCTION_DEFINITION`.

| Propriedade | Lambda — Cálculo | Lambda — Exportação |
|---|---|---|
| Nome sugerido | `calculadora-juros-compostos` | `exportar-simulacao` |
| Função Spring | `calculationFunction` | `exportFunction` |
| Runtime | Java 21 | Java 21 |
| Handler | `FunctionInvoker::handleRequest` | `FunctionInvoker::handleRequest` |
| JAR | `calculadora-juros-compostos-1.0.0.jar` | **mesmo JAR** |
| Memória | 512 MB | 512 MB |
| Timeout | 30 s | 30 s |
| SnapStart | Recomendado | Recomendado |

> **Como funciona:** o Spring Boot registra ambos os beans (`calculationFunction` e `exportFunction`) no contexto. A variável de ambiente `SPRING_CLOUD_FUNCTION_DEFINITION` instrui o `FunctionInvoker` sobre qual bean usar para processar as requisições daquela Lambda.

---

### DynamoDB

| Propriedade | Valor |
|---|---|
| Tabela | `simulations` |
| Partition Key (PK) | `simulationId` (UUID String) |
| TTL | Campo `ttl` (Unix epoch) — expira em 90 dias |
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
| Apache POI (poi-ooxml) | 5.2.5 | Geração de planilhas .xlsx |
| Lombok | 1.18.38 | Redução de boilerplate |
| Jackson | 2.x | Serialização JSON |
| JUnit 5 + Mockito | — | Testes unitários |
| Maven Shade Plugin | 3.5.1 | Fat JAR para deploy |

### Estrutura de Pacotes

```
com.example.calculadora
├── CalculadoraApplication.java               # Entry point Spring Boot
├── adapter
│   ├── in
│   │   ├── CalculationFunction.java          # Function<Message<Req>, Res> — cálculo
│   │   ├── CalculationRequest.java           # Payload de entrada do cálculo
│   │   ├── CalculationResponse.java          # Payload de saída do cálculo
│   │   ├── ExportFunction.java               # Function<Message<Req>, Res> — exportação
│   │   ├── ExportRequest.java                # Payload de entrada da exportação
│   │   └── ExportResponse.java               # Payload de saída (fileName + Base64)
│   └── out
│       └── DynamoSimulationRepository.java   # SaveSimulationPort + GetSimulationPort
├── application
│   ├── port
│   │   ├── CalculateInterestUseCase.java
│   │   ├── SaveSimulationPort.java
│   │   ├── GetSimulationPort.java            # Leitura por simulationId
│   │   └── ExportSimulationUseCase.java      # Geração do relatório Excel
│   └── service
│       ├── CompoundInterestService.java      # Orquestra cálculo + persistência
│       └── ExcelExportService.java           # Gera .xlsx com Apache POI
├── config
│   └── FunctionConfig.java                   # Registra calculationFunction + exportFunction
└── domain
    ├── model
    │   ├── Simulation.java                   # Entidade DynamoDB (@DynamoDbBean)
    │   ├── SimulationInterval.java           # Intervalo de aporte (@DynamoDbBean)
    │   ├── SimulationResult.java             # Resultado completo (inclui simulationId)
    │   └── IntervalResult.java               # Resultado por intervalo (@DynamoDbBean)
    └── strategy
        ├── IntervalStrategy.java             # Interface Strategy
        ├── SimpleIntervalStrategy.java       # Aporte mensal fixo
        ├── ExtraContributionStrategy.java    # Aporte mensal + extra
        └── ZeroContributionStrategy.java     # Sem aporte (pausa)
```

### Padrão Strategy — Resolução de Estratégia

O `CompoundInterestService` escolhe automaticamente a estratégia correta para cada intervalo:

```
extraContribution > 0    →  ExtraContributionStrategy
monthlyContribution > 0  →  SimpleIntervalStrategy
caso contrário           →  ZeroContributionStrategy
```

### Geração de Excel — ExcelExportService

A `ExportFunction` recebe o `simulationId`, busca a simulação no DynamoDB via `GetSimulationPort` e gera uma planilha `.xlsx` com três abas:

| Aba | Conteúdo |
|---|---|
| **Resumo** | ID, data, usuário, taxa anual, valor inicial, total investido, total em juros, valor final, rendimento % |
| **Intervalos** | Aportes mensais, duração e aportes extras de cada intervalo informado |
| **Evolução** | Total aportado, juros gerados e saldo final por intervalo + linha de totais |

O arquivo é retornado codificado em **Base64** dentro do JSON de resposta. O frontend decodifica e dispara o download diretamente no navegador.

---

## Frontend

Aplicação single-page (`index.html`) hospedada como site estático no **Amazon S3**.

| Propriedade | Valor |
|---|---|
| Stack | HTML5 + CSS3 + JavaScript puro (sem frameworks) |
| Hospedagem | S3 Static Website Hosting |
| Tema | Dark mode (bg `#12121e`, destaque laranja `#f97316`) |
| Responsividade | Breakpoints em 560 px e 480 px |

### Funcionalidades

- Formulário com valor inicial e taxa anual
- Adição e remoção dinâmica de intervalos (numeração automática reordenada)
- Aporte extra opcional por intervalo (a partir do segundo)
- Simulação com exibição de totais, barras de progresso e tabela por intervalo
- Botão **Exportar Excel** — aparece após a simulação e dispara download do `.xlsx`
- Timer de elapsed time durante a chamada à API (hint de cold start após 5 s)
- UUID persistido em `localStorage` como identificador de usuário

### Configuração

Edite as constantes no topo do `<script>` de `frontend/index.html`:

```javascript
const API_URL    = 'https://<id>.execute-api.sa-east-1.amazonaws.com/prod/calcular';
const EXPORT_URL = 'https://<id>.execute-api.sa-east-1.amazonaws.com/prod/exportar';
```

### Deploy para o S3

```bash
aws s3 cp frontend/index.html s3://<seu-bucket>/ --content-type "text/html"
```

---

## API Reference

### `POST /prod/calcular`

Calcula a projeção de juros compostos e persiste a simulação no DynamoDB.

**URL**
```
https://13zv2pr424.execute-api.sa-east-1.amazonaws.com/prod/calcular
```

**Headers**

| Header | Obrigatório | Descrição |
|---|---|---|
| `Content-Type` | Sim | `application/json` |
| `X-User-Id` | Não | Identificador do usuário. Default: `anonymous` |

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
| `intervals[].monthlyContribution` | `number` | Aporte mensal do intervalo (R$) |
| `intervals[].periodInMonths` | `integer` | Duração do intervalo em meses |
| `intervals[].extraContribution` | `number\|null` | Aporte extra injetado no início do intervalo (R$) |

**Response — 200 OK**

```json
{
  "simulationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "totalInvested": 46000.00,
  "totalInterest": 12345.67,
  "finalValue": 58345.67,
  "intervals": [
    {
      "finalBalance": 16823.45,
      "totalContributed": 6000.00,
      "interestEarned": 823.45
    },
    {
      "finalBalance": 58345.67,
      "totalContributed": 34000.00,
      "interestEarned": 11522.22
    }
  ]
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `simulationId` | `string` | UUID da simulação persistida — usado para exportação |
| `totalInvested` | `number` | Valor inicial + soma de todos os aportes |
| `totalInterest` | `number` | Total de juros gerados |
| `finalValue` | `number` | Saldo final |
| `intervals[].finalBalance` | `number` | Saldo ao final do intervalo |
| `intervals[].totalContributed` | `number` | Total aportado no intervalo |
| `intervals[].interestEarned` | `number` | Juros gerados no intervalo |

---

### `POST /prod/exportar`

Busca uma simulação pelo ID e retorna um relatório `.xlsx` codificado em Base64.

**URL**
```
https://<export-api-id>.execute-api.sa-east-1.amazonaws.com/prod/exportar
```

**Request Body**

```json
{
  "simulationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Response — 200 OK**

```json
{
  "fileName": "simulacao-a1b2c3d4.xlsx",
  "fileContent": "UEsDBBQABgAIAAAAIQB..."
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `fileName` | `string` | Nome sugerido para o arquivo |
| `fileContent` | `string` | Conteúdo do `.xlsx` codificado em Base64 |

**Exemplo de teste (console AWS)**

```json
{
  "httpMethod": "POST",
  "path": "/exportar",
  "requestContext": { "httpMethod": "POST", "resourcePath": "/exportar" },
  "headers": { "Content-Type": "application/json" },
  "body": "{\"simulationId\":\"SEU_ID_AQUI\"}",
  "isBase64Encoded": false
}
```

**Exemplo com curl**

```bash
curl -X POST https://<export-api-id>.execute-api.sa-east-1.amazonaws.com/prod/exportar \
  -H "Content-Type: application/json" \
  -d '{"simulationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"}' \
  | jq -r '.fileContent' | base64 -d > simulacao.xlsx
```

---

## Modelo de Dados — DynamoDB

### Tabela `simulations`

| Atributo | Tipo DynamoDB | Descrição |
|---|---|---|
| `simulationId` | String **(PK)** | UUID gerado pela aplicação |
| `userId` | String | Identificador do usuário (header `X-User-Id`) |
| `createdAt` | String | Timestamp ISO-8601 da criação |
| `ttl` | Number | Unix epoch de expiração (90 dias após criação) |
| `initialValue` | Number | Valor inicial da simulação |
| `annualRate` | Number | Taxa anual informada (%) |
| `intervals` | List\<Map\> | Intervalos de aporte informados pelo usuário |
| `intervals[].monthlyContribution` | Number | Aporte mensal do intervalo |
| `intervals[].periodInMonths` | Number | Duração em meses |
| `intervals[].extraContribution` | Number | Aporte extra (quando aplicável) |
| `totalInvested` | Number | Total investido calculado |
| `totalInterest` | Number | Total de juros calculado |
| `finalValue` | Number | Saldo final calculado |
| `intervalResults` | List\<Map\> | Resultado calculado por intervalo |
| `intervalResults[].totalContributed` | Number | Total aportado no intervalo |
| `intervalResults[].interestEarned` | Number | Juros gerados no intervalo |
| `intervalResults[].finalBalance` | Number | Saldo final do intervalo |

> Os campos `intervals` e `intervalResults` são listas de mapas armazenadas nativamente pelo DynamoDB Enhanced Client a partir das classes `@DynamoDbBean` `SimulationInterval` e `IntervalResult`.

---

## Fórmulas Financeiras

### Conversão de Taxa

A taxa anual informada é convertida para taxa mensal equivalente usando **taxa efetiva**:

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
FV = SimpleIntervalStrategy(saldo_ajustado, PMT, r, n)
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

> O mesmo JAR é usado para **ambas as Lambdas**.

---

### Deploy — Lambda de Cálculo

1. AWS Console → Lambda → `calculadora-juros-compostos`
2. **Code → Upload from → .jar file** → selecione o JAR gerado
3. Verifique as variáveis de ambiente (seção abaixo)

```bash
# Ou via CLI:
aws lambda update-function-code \
  --function-name calculadora-juros-compostos \
  --zip-file fileb://target/calculadora-juros-compostos-1.0.0.jar \
  --region sa-east-1
```

---

### Deploy — Lambda de Exportação

1. AWS Console → Lambda → **Criar função**
   - Nome: `exportar-simulacao`
   - Runtime: Java 21
   - Handler: `org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest`
2. **Code → Upload from → .jar file** → **mesmo JAR** da Lambda de cálculo
3. Configure as variáveis de ambiente (seção abaixo)
4. Adicione permissão IAM: `dynamodb:GetItem` na tabela `simulations`
5. Crie rota `POST /exportar` no API Gateway apontando para esta Lambda

---

## Variáveis de Ambiente

### Lambda de Cálculo

| Variável | Valor | Descrição |
|---|---|---|
| `SPRING_CLOUD_FUNCTION_DEFINITION` | `calculationFunction` | Seleciona a função de cálculo (padrão do `application.yml`) |
| `DYNAMODB_TABLE_NAME` | `simulations` | Nome da tabela DynamoDB |
| `AWS_REGION` | `sa-east-1` | Região dos serviços AWS |

### Lambda de Exportação

| Variável | Valor | Descrição |
|---|---|---|
| `SPRING_CLOUD_FUNCTION_DEFINITION` | `exportFunction` | **Obrigatório** — seleciona a função de exportação |
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

### Simulação local da Lambda de cálculo

```bash
mvn test -Dtest=LambdaGatewaySimulationTest
```

Instancia toda a cadeia (sem Spring context, sem conexão AWS) e imprime o JSON de resposta no console.

### Teste manual da Lambda de exportação (console AWS)

```json
{
  "httpMethod": "POST",
  "path": "/exportar",
  "requestContext": { "httpMethod": "POST", "resourcePath": "/exportar" },
  "headers": { "Content-Type": "application/json" },
  "body": "{\"simulationId\":\"SEU_ID_AQUI\"}",
  "isBase64Encoded": false
}
```

---

## Configurações de Segurança e Limites

### Rate Limiting — API Gateway

| Nível | Rate | Burst |
|---|---|---|
| API global | 100 req/s | 200 |
| `POST /calcular` | 50 req/s | 100 |
| `POST /exportar` | 50 req/s | 100 |

### Budget Alert — AWS Billing

Alerta configurado em **AWS Budgets** para notificar por email ao atingir 85% e 100% do limite mensal definido.

### IAM — Permissões das Lambdas

**Lambda de Cálculo**

```json
{
  "Effect": "Allow",
  "Action": ["dynamodb:PutItem"],
  "Resource": "arn:aws:dynamodb:sa-east-1:*:table/simulations"
}
```

**Lambda de Exportação**

```json
{
  "Effect": "Allow",
  "Action": ["dynamodb:GetItem"],
  "Resource": "arn:aws:dynamodb:sa-east-1:*:table/simulations"
}
```

### TTL — Dados expiram automaticamente

Simulações são removidas automaticamente do DynamoDB após **90 dias** via TTL nativo, sem custo adicional de operação.
