# 🚀 Serviço de Motor de Compras

Microserviço responsável por executar o motor de compra programada de ações, gerenciar distribuições proporcionais aos clientes, controlar resíduos na conta master e processar rebalanceamentos de carteira.

---

## 📋 Índice

- [Conceito Geral](#-conceito-geral)
- [Arquitetura](#-arquitetura)
- [Configuração Inicial](#-configuração-inicial)
- [Fluxos Principais](#-fluxos-principais)
- [Endpoints Disponíveis](#-endpoints-disponíveis)
- [Eventos Kafka](#-eventos-kafka)
- [Modelos de Dados](#-modelos-de-dados)
- [Regras de Negócio](#-regras-de-negócio)

---

## 🎯 Conceito Geral

O **Motor de Compras** é o coração do sistema de investimentos automatizados. Ele executa compras programadas de ações seguindo uma estratégia de **Dollar Cost Averaging (DCA)**, distribuindo proporcionalmente os ativos adquiridos entre os clientes investidores.

### Principais Responsabilidades

#### 1️⃣ **Execução de Compras Programadas**
- Agrega aportes mensais de todos os clientes ativos
- Divide o valor mensal em 3 parcelas (dias 5, 15 e 25)
- Compra ativos da cesta ativa seguindo os percentuais definidos
- Respeita dias úteis (segunda a sexta-feira)

#### 2️⃣ **Distribuição Proporcional**
- Distribui ações compradas proporcionalmente ao aporte de cada cliente
- Utiliza **truncamento** para evitar frações de centavo
- Mantém resíduos não distribuídos na conta master
- Atualiza preço médio de aquisição de cada cliente

#### 3️⃣ **Gestão de Resíduos**
- Armazena ações não distribuídas na **Custódia Master**
- Reutiliza resíduos em compras futuras (descontando da quantidade a comprar)
- Minimiza desperdício e otimiza o capital investido

#### 4️⃣ **Rebalanceamento de Carteira**
- Processa mudanças na cesta ativa (Top 5 ativos)
- **Vende** 100% dos ativos que saíram da cesta
- **Compra** ativos novos que entraram na cesta
- **Rebalancea** ativos que mudaram de percentual
- Publica eventos de IR para compliance fiscal

#### 5️⃣ **Controle Fiscal (IR)**
- Publica eventos de **IR Dedo-Duro** (0,005% sobre cada operação)
- Calcula **IR sobre vendas mensais** > R$ 20.000 (20% sobre lucro)
- Envia eventos para sistemas de declaração fiscal

---

## 🏗 Arquitetura

### Padrão Arquitetural
- **Event-Driven Architecture**: Comunicação via Kafka
- **Microserviços**: Serviço independente e desacoplado
- **Domain-Driven Design**: Organização por domínios de negócio

### Stack Tecnológica

```
├── Spring Boot 3.4.5       # Framework principal
├── Spring Data JPA         # Persistência de dados
├── PostgreSQL 16           # Banco de dados relacional
├── Apache Kafka            # Mensageria assíncrona
├── Spring Cloud OpenFeign  # Cliente HTTP declarativo
├── Springdoc OpenAPI 2.8.0 # Documentação Swagger
└── JUnit 5 + Mockito       # Testes unitários
```

### Integrações Externas (via Feign Clients)

```
Motor de Compras
    ├── srv.clientes         → Buscar clientes ativos e aportes
    ├── srv.cesta            → Obter cesta ativa e histórico
    ├── srv.contas-graficas  → Gerenciar contas master/filhote
    └── srv.cotacoes         → Consultar cotações B3 (COTAHIST)
```

### Eventos Kafka

**Produz:**
- `ir-dedo-duro` - IR de 0,005% sobre cada operação
- `ir-vendas-mensais` - IR de 20% sobre lucro em vendas > R$ 20k
- `rebalanceamento-cesta` - Trigger para rebalanceamento

**Consome:**
- `rebalanceamento-cesta` - Processa rebalanceamento de carteira

---

## ⚙️ Configuração Inicial

### 📦 Pré-requisitos

1. **Java 17** instalado
2. **Maven 3.8+** instalado
3. **PostgreSQL 16** rodando na porta 5433
4. **Apache Kafka** rodando na porta 9092
5. **Common Library** instalada localmente

### 📚 Instalação da Common Library

⚠️ **IMPORTANTE**: Este projeto depende da biblioteca compartilhada `itau-common-library` que deve estar instalada no repositório Maven local antes de compilar o projeto.

```bash
# Clone o repositório da common library
git clone https://github.com/desafio-itau/itau-common-library.git
cd itau-common-library

# Instale no repositório Maven local
mvn clean install

# Verifique a instalação
ls ~/.m2/repository/com/itau/common.library/
```

Se a biblioteca não estiver instalada, você verá o erro:
```
Could not resolve dependencies for project com.itau:srv.motor.compras
```

### 🔧 Variáveis de Ambiente

Crie um arquivo `.env` na pasta `env/` com as seguintes configurações:

```properties
# Database
DESAFIO_ITAU_DB_NAME=desafio_itau_db
DESAFIO_ITAU_DB_USER=postgres
DESAFIO_ITAU_DB_PASSWORD=postgres

# Motor de Compras
MOTOR_COMPRAS_CONTA_MASTER_ID=1

# Serviços Externos
ITAU_SRV_CLIENTES_URL=http://localhost:8080/api/clientes
ITAU_SRV_CESTA_URL=http://localhost:8081/api/admin
ITAU_SRV_CONTA_GRAFICA_URL=http://localhost:8080/api/contas-graficas
ITAU_SRV_COTACOES_URL=http://localhost:8081/api/cotacoes
```

### 🗄️ Estrutura do Banco de Dados

O serviço utiliza as seguintes tabelas principais:

```sql
-- Ordens de compra executadas
CREATE TABLE ordens_compra (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL,
    quantidade_total INTEGER NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    valor_total DECIMAL(15,2) NOT NULL,
    data_execucao DATE NOT NULL
);

-- Custódia de ativos (master e filhote)
CREATE TABLE custodias (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_medio DECIMAL(10,2) NOT NULL,
    conta_grafica_id BIGINT NOT NULL,
    data_ultima_atualizacao TIMESTAMP NOT NULL
);

-- Distribuições realizadas
CREATE TABLE distribuicoes (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    ordem_compra_id BIGINT NOT NULL,
    custodia_filhote_id BIGINT NOT NULL,
    quantidade_distribuida INTEGER NOT NULL,
    valor_aporte DECIMAL(15,2) NOT NULL,
    data_distribuicao TIMESTAMP NOT NULL
);

-- Eventos de IR (dedo-duro e vendas)
CREATE TABLE eventos_ir (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    cpf VARCHAR(14) NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    tipo_operacao VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    valor_base DECIMAL(15,2) NOT NULL,
    valor_ir DECIMAL(15,2) NOT NULL,
    data_evento TIMESTAMP NOT NULL
);
```

### 🚀 Porta do Serviço

O serviço roda na porta **8082** por padrão.

---

## 🔄 Fluxos Principais

### 1. Motor de Compra Programada

```
POST /api/motor/executar-compra
{
  "dataReferencia": "2026-03-05"
}
```

**Fluxo Detalhado:**

#### Passo 1: Agrupamento de Clientes
```
1. Buscar todos os clientes com ativo = true
2. Calcular: valorPorCliente = valorMensal / 3
3. Somar: totalConsolidado = Σ(valoresPorCliente)
```

**Exemplo:**
```
Cliente A: R$ 3.000/mês ÷ 3 = R$ 1.000
Cliente B: R$ 6.000/mês ÷ 3 = R$ 2.000  
Cliente C: R$ 1.500/mês ÷ 3 = R$ 500
───────────────────────────────────────
Total Consolidado = R$ 3.500
```

#### Passo 2: Calcular Quantidade por Ativo
```
Para cada ativo da cesta ativa:
  1. valorPorAtivo = totalConsolidado × percentual
  2. Buscar cotação de fechamento (COTAHIST)
  3. quantidadeBruta = TRUNCAR(valorPorAtivo / cotação)
  4. Descontar saldo da Custodia Master (resíduos anteriores)
  5. quantidadeFinal = quantidadeBruta - residuoMaster
```

**Exemplo:**
```
Ativo: PETR4 (30% da cesta)
Valor: R$ 3.500 × 30% = R$ 1.050
Cotação: R$ 35,50
Quantidade Bruta: TRUNCAR(1.050 / 35,50) = 29 ações
Resíduo Master: 2 ações
Quantidade a Comprar: 29 - 2 = 27 ações
```

#### Passo 3: Separar Lote Padrão vs Fracionário
```
Se quantidade >= 100:
  - Lote Padrão: múltiplos de 100 (ticker sem sufixo)
  - Fracionário: resto < 100 (ticker + "F")
  
Se quantidade < 100:
  - Tudo fracionário (ticker + "F")
```

**Exemplo:**
```
350 ações → 300 PETR4 + 50 PETR4F
28 ações → 28 PETR4F
```

#### Passo 4: Distribuição Proporcional
```
Total Disponível = compradas + residuoMasterAnterior

Para cada cliente:
  1. proporção = aporteCliente / totalConsolidado
  2. quantidade = TRUNCAR(proporção × totalDisponível)
  3. Atualizar Custodia Filhote
  4. Recalcular Preço Médio:
     PM = (QtdAnterior × PMAnterior + QtdNova × PreçoNova) / (QtdAnterior + QtdNova)
```

**Exemplo:**
```
Total Disponível: 100 ações PETR4
Cliente A (aporte R$ 1.000 de R$ 3.500 = 28,57%):
  Quantidade = TRUNCAR(100 × 0,2857) = 28 ações

Cliente B (aporte R$ 2.000 de R$ 3.500 = 57,14%):
  Quantidade = TRUNCAR(100 × 0,5714) = 57 ações

Cliente C (aporte R$ 500 de R$ 3.500 = 14,29%):
  Quantidade = TRUNCAR(100 × 0,1429) = 14 ações

Resíduo Master: 100 - (28 + 57 + 14) = 1 ação
```

#### Passo 5: Atualização de Resíduos
```
Ações não distribuídas ficam na Custodia Master
Serão descontadas na próxima compra (Passo 2.4)
```

#### Passo 6: Publicar Eventos de IR
```
Para cada distribuição:
  1. IR Dedo-Duro = valorOperacao × 0,00005 (0,005%)
  2. Publicar evento no Kafka
```

---

### 2. Rebalanceamento de Carteira

```
POST /api/rebalanceamentos/eventos
{
  "cestaAnteriorId": 1,
  "cestaAtualId": 2,
  "dataExecucao": "2026-03-05T10:00:00"
}
```

**Fluxo Detalhado:**

#### Etapa 1: Análise de Mudanças
```
1. Buscar cesta anterior e cesta atual
2. Identificar:
   - Ativos que SAÍRAM da cesta
   - Ativos que ENTRARAM na cesta
   - Ativos que PERMANECERAM com % alterado
```

#### Etapa 2-3: Processar Vendas (Ativos que Saíram)
```
Para cada ativo que saiu:
  1. Buscar custódia do cliente
  2. Vender 100% das ações
  3. Calcular lucro: (preçoVenda - preçoMédio) × quantidade
  4. Zerar custódia
  5. Publicar IR Dedo-Duro (0,005%)
```

#### Etapa 4: Consolidar Vendas Mensais
```
Se totalVendas > R$ 20.000:
  1. Calcular lucroTotal
  2. IR = lucroTotal × 20%
  3. Publicar evento IR Vendas Mensais
```

#### Etapa 5-7: Processar Compras (Ativos que Entraram)
```
saldoDisponível = totalArrecadadoVendas

Para cada novo ativo:
  1. Calcular valor: saldoDisponível × percentualAtivo
  2. Buscar cotação
  3. Calcular quantidade a comprar
  4. Separar lote padrão e fracionário
  5. Atualizar custódia do cliente
```

#### Etapa 8-10: Rebalancear Ativos Permanecidos
```
Para cada ativo que permaneceu:
  Se percentualAnterior ≠ percentualNovo:
    1. Calcular quantidade alvo: (valorCarteira × percentualNovo) / cotação
    2. Se quantidadeAtual > quantidadeAlvo: VENDER diferença
    3. Se quantidadeAtual < quantidadeAlvo: COMPRAR diferença
    4. Atualizar custódia
```

---

## 📡 Endpoints Disponíveis

### Motor de Compra

#### `POST /api/motor/executar-compra`
Executa o motor de compra programada.

**Request:**
```json
{
  "dataReferencia": "2026-03-05"
}
```

**Response:**
```json
{
  "dataExecucao": "2026-03-05T10:30:00",
  "totalClientes": 3,
  "totalConsolidado": 3500.00,
  "ordensCompra": [
    {
      "ticker": "PETR4",
      "quantidadeTotal": 350,
      "detalhes": [
        {
          "tipo": "LOTE_PADRAO",
          "ticker": "PETR4",
          "quantidade": 300
        },
        {
          "tipo": "FRACIONARIO",
          "ticker": "PETR4F",
          "quantidade": 50
        }
      ],
      "precoUnitario": 35.50,
      "valorTotal": 12425.00
    }
  ],
  "distribuicoes": [
    {
      "clienteId": 1,
      "nome": "João Silva",
      "valorAporte": 1000.00,
      "ativos": [
        {
          "ticker": "PETR4",
          "quantidade": 28
        }
      ]
    }
  ],
  "residuosCustMaster": [
    {
      "ticker": "PETR4",
      "quantidade": 2
    }
  ],
  "eventosIRPublicados": 15,
  "mensagem": "Motor de compra executado com sucesso"
}
```

---

### Custódia

#### `GET /api/custodias/{clienteId}`
Retorna a custódia de um cliente específico.

**Response:**
```json
[
  {
    "ticker": "PETR4",
    "quantidade": 100,
    "precoMedio": 35.50,
    "valorAtual": 36.00,
    "origem": "Resíduo distribuição 2026-03-01"
  }
]
```

#### `GET /api/custodia-master`
Retorna a custódia master (resíduos).

**Response:**
```json
{
  "contaMaster": {
    "id": 1,
    "numeroConta": "ITAUMASTER",
    "tipo": "MASTER"
  },
  "custodia": [
    {
      "ticker": "PETR4",
      "quantidade": 2,
      "precoMedio": 35.50,
      "valorAtual": 36.00,
      "origem": "Resíduo distribuição 2026-03-05"
    }
  ],
  "valorTotalResiduo": 72.00
}
```

---

### Valores

#### `GET /api/valores/{clienteId}`
Retorna valores investidos e vendidos por um cliente.

**Response:**
```json
{
  "valorInvestido": 5000.00,
  "valorVendido": 1500.00
}
```

#### `GET /api/valores?clienteId=1&data=2026-03-05`
Retorna valores em uma data específica.

**Response:**
```json
{
  "dataEvento": "2026-03-05",
  "valores": {
    "valorInvestido": 1000.00,
    "valorVendido": 0.00
  }
}
```

#### `GET /api/valores/historico/{clienteId}`
Retorna histórico de aportes.

**Response:**
```json
[
  {
    "data": "2026-03-05",
    "valor": 1000.00,
    "parcela": "1/3"
  },
  {
    "data": "2026-03-15",
    "valor": 1000.00,
    "parcela": "2/3"
  }
]
```

---

### Rebalanceamento

#### `POST /api/rebalanceamentos/eventos`
Publica evento de rebalanceamento no Kafka.

**Request:**
```json
{
  "cestaAnteriorId": 1,
  "cestaAtualId": 2,
  "dataExecucao": "2026-03-05T10:00:00"
}
```

**Response:**
```
204 No Content
```

---

## 📨 Eventos Kafka

### Produzidos

#### `ir-dedo-duro`
Evento de IR de 0,005% sobre cada operação.

```json
{
  "clienteId": 1,
  "cpf": "123.456.789-00",
  "ticker": "PETR4",
  "tipoOperacao": "COMPRA",
  "quantidade": 100,
  "precoUnitario": 35.50,
  "valorBase": 3550.00,
  "valorIR": 0.18,
  "dataEvento": "2026-03-05T10:30:00"
}
```

#### `ir-vendas-mensais`
Evento de IR de 20% sobre lucro em vendas > R$ 20.000.

```json
{
  "clienteId": 1,
  "cpf": "123.456.789-00",
  "mesReferencia": "2026-03",
  "totalVendido": 25000.00,
  "lucroTotal": 5000.00,
  "valorIR": 1000.00,
  "vendas": [
    {
      "ticker": "ABEV3",
      "quantidade": 100,
      "precoVenda": 12.00,
      "precoMedio": 11.00,
      "lucro": 100.00
    }
  ],
  "dataEvento": "2026-03-05T10:30:00"
}
```

### Consumidos

#### `rebalanceamento-cesta`
Consome eventos de mudança de cesta para processar rebalanceamento.

```json
{
  "cestaAnteriorId": 1,
  "cestaAtualId": 2,
  "dataExecucao": "2026-03-05T10:00:00"
}
```

---

## ⚖️ Regras de Negócio

### 1. Dias de Execução
- ✅ Compras ocorrem nos dias **5, 15 e 25** de cada mês
- ✅ Apenas em **dias úteis** (segunda a sexta-feira)
- ❌ Se cair em fim de semana, **não executa** (aguarda próximo dia útil)

### 2. Cálculo de Quantidades
- ✅ Sempre usar **TRUNCAR** (arredondar para baixo)
- ✅ Nunca frações de ações
- ✅ Resíduos ficam na conta master

### 3. Lote Padrão vs Fracionário
- ✅ `>= 100 ações`: Dividir em lote padrão + fracionário
- ✅ `< 100 ações`: Tudo fracionário
- ✅ Fracionário sempre com sufixo **"F"** no ticker

### 4. Distribuição Proporcional
- ✅ Proporção baseada no aporte de cada cliente
- ✅ Truncar quantidade distribuída
- ✅ Resíduos não distribuídos ficam na master

### 5. Preço Médio
```
PM = (QtdAnterior × PMAnterior + QtdNova × PreçoNova) / (QtdAnterior + QtdNova)
```

### 6. IR Dedo-Duro
- ✅ **0,005%** sobre cada operação (compra ou venda)
- ✅ Evento publicado para **cada** operação
- ✅ Valor mínimo: R$ 0,01

### 7. IR Vendas Mensais
- ✅ Aplicado **apenas** se total vendido no mês > **R$ 20.000**
- ✅ Alíquota: **20%** sobre o lucro líquido
- ✅ Consolida todas as vendas do mês
- ✅ Um único evento por cliente por mês

### 8. Rebalanceamento
- ✅ Vender **100%** dos ativos que saíram
- ✅ Comprar ativos novos com o valor arrecadado
- ✅ Rebalancear ativos com % alterado
- ✅ Publicar eventos de IR para todas as operações

### 9. Validações
- ❌ Não permitir compra duplicada na mesma data
- ❌ Não permitir execução em dias não úteis
- ❌ Validar existência de cesta ativa
- ❌ Validar clientes ativos disponíveis

---

## 📚 Documentação Adicional

### Swagger UI
Após iniciar o serviço, acesse a documentação interativa:

```
http://localhost:8082/swagger-ui.html
```

### OpenAPI JSON
Especificação OpenAPI 3.0:

```
http://localhost:8082/v3/api-docs
```

### Cobertura de Testes
Após executar `mvn test`, visualize o relatório JaCoCo:

```
target/site/jacoco/index.html
```

**Meta de cobertura:** ≥ 90% (configurado no `pom.xml`)

---

## 🔍 Monitoramento e Logs

### Logs Estruturados
O serviço utiliza SLF4J com Logback para logs estruturados:

```
2026-03-05 10:30:00 INFO  [MotorCompraService] - Iniciando motor de compra para data: 2026-03-05
2026-03-05 10:30:05 INFO  [MotorCompraService] - Total clientes ativos: 3
2026-03-05 10:30:10 INFO  [MotorCompraService] - Total consolidado: R$ 3.500,00
2026-03-05 10:30:15 INFO  [MotorCompraService] - Ordens de compra criadas: 5
2026-03-05 10:30:20 INFO  [MotorCompraService] - Distribuições realizadas: 15
2026-03-05 10:30:25 INFO  [MotorCompraService] - Eventos IR publicados: 15
2026-03-05 10:30:30 INFO  [MotorCompraService] - Motor de compra finalizado com sucesso
```

### Métricas Importantes
- Total de clientes processados
- Valor total consolidado
- Quantidade de ordens criadas
- Quantidade de distribuições
- Quantidade de resíduos
- Eventos Kafka publicados

---