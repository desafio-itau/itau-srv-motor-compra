# 📋 PASSO A PASSO - REBALANCEAMENTO POR MUDANÇA DE CESTA

## ETAPA 1: Endpoint de Alteração de Cesta (Admin)

**Endpoint:** `PUT /api/admin/cesta`

**O que implementar:**
1. Receber a nova cesta (5 ativos com percentuais que somam 100%)
2. Validar:
   - Exatamente 5 ativos
   - Soma dos percentuais = 100%
   - Cada percentual > 0%
3. **Desativar** a cesta anterior (preencher `data_desativacao`)
4. **Criar** a nova cesta ativa
5. **Disparar o processo de rebalanceamento** assíncrono (pode ser via Kafka ou chamada direta)

---

## ETAPA 2: Análise Comparativa (Lógica de Negócio)

Para **cada cliente ativo**, você precisa identificar 3 categorias de ativos:

### 2.1. Ativos que SAÍRAM da cesta
- Estão na cesta antiga MAS NÃO estão na nova
- **Ação:** VENDER 100% da posição do cliente

### 2.2. Ativos que ENTRARAM na cesta
- NÃO estão na cesta antiga MAS estão na nova
- **Ação:** COMPRAR com o dinheiro das vendas

### 2.3. Ativos que PERMANECERAM mas mudaram de percentual
- Estão em ambas as cestas
- **Ação:** REBALANCEAR (vender excesso ou comprar deficit)

---

## ETAPA 3: Venda de Ativos que Saíram

Para cada cliente ativo:

### 3.1. Buscar a custodia do cliente
- Identificar todos os ativos que ele possui

### 3.2. Para cada ativo que saiu da cesta:
- Obter cotação atual do ativo (último fechamento no COTAHIST)
- Quantidade a vender = quantidade total na custodia do cliente
- Valor obtido = quantidade × cotação atual
- **Registrar operação de VENDA:**
  - Atualizar custodia (zerar quantidade desse ativo)
  - Calcular lucro = (cotação atual - preço médio) × quantidade
  - Preço médio **NÃO se altera** na venda
  - Acumular o valor obtido com as vendas

### 3.3. Calcular IR sobre vendas:
- Somar **todas as vendas do mês** do cliente
- Se total vendas > R$ 20.000,00:
  - Calcular lucro líquido total
  - Se lucro > 0: IR = lucro × 20%
  - **Publicar no Kafka** (tópico `ir-vendas`)

### 3.4. Publicar IR Dedo-Duro:
- Para cada venda individual
- IR = valor operação × 0,005%
- **Publicar no Kafka** (tópico `ir-dedo-duro`)

---

## ETAPA 4: Cálculo do Valor Disponível para Compra

Para cada cliente:

### 4.1. Total disponível para realocar:
```
Total = Σ (valor de todas as vendas de ativos que saíram)
```

### 4.2. Distribuir proporcionalmente entre novos ativos:
- Para cada ativo que **ENTROU**:
  - Percentual do novo ativo na nova cesta
  - Valor a investir = (percentual do ativo / soma % dos novos ativos) × total disponível

**Exemplo:**
- Saíram: BBDC4 (15%) e WEGE3 (10%) → total vendas = R$ 230
- Entraram: ABEV3 (20%) e RENT3 (15%)
- ABEV3: 20/(20+15) × 230 = 57,14% × 230 = R$ 131,43
- RENT3: 15/(20+15) × 230 = 42,86% × 230 = R$ 98,57

---

## ETAPA 5: Compra de Novos Ativos

Para cada novo ativo:

### 5.1. Obter cotação atual (COTAHIST)

### 5.2. Calcular quantidade a comprar:
```
Quantidade = TRUNCAR(valor disponível / cotação)
```

### 5.3. Separar lote padrão vs fracionário:
- Se quantidade >= 100:
  - Lote padrão = (quantidade / 100) × 100
  - Fracionário = quantidade % 100
  - Ticker lote padrão: ex: `ABEV3`
  - Ticker fracionário: ex: `ABEV3F`
- Se quantidade < 100:
  - Tudo no mercado fracionário com sufixo "F"

### 5.4. Registrar operação de COMPRA:
- Atualizar custodia do cliente
- **Recalcular preço médio:**
  ```
  PM novo = (qtd_anterior × PM_anterior + qtd_nova × preço_novo) / (qtd_anterior + qtd_nova)
  ```
  - Se for primeira compra do ativo: PM = preço de compra

### 5.5. Publicar IR Dedo-Duro:
- IR = valor operação × 0,005%
- **Publicar no Kafka**

---

## ETAPA 6: Rebalanceamento de Ativos que Permaneceram

Para ativos que estão em **ambas as cestas** mas mudaram de percentual:

### 6.1. Calcular valor alvo do ativo:
```
Valor total da carteira atual do cliente = Σ (qtd × cotação) de todos os ativos
Valor alvo do ativo = valor total × novo percentual do ativo
Quantidade alvo = TRUNCAR(valor alvo / cotação atual)
```

### 6.2. Comparar com quantidade atual:
- Se quantidade atual > quantidade alvo:
  - **VENDER** excesso
  - Quantidade a vender = qtd atual - qtd alvo
  - Seguir os mesmos passos de venda (ETAPA 3)
  
- Se quantidade atual < quantidade alvo:
  - **COMPRAR** deficit
  - Valor necessário = (qtd alvo - qtd atual) × cotação
  - Seguir os mesmos passos de compra (ETAPA 5)
  - **ATENÇÃO:** Precisa ter saldo disponível (do caixa das vendas ou considerar novo aporte)

### 6.3. Registrar operações e IR:
- Publicar IR dedo-duro para cada operação
- Acumular vendas para calcular IR sobre vendas do mês

---

## ETAPA 7: Atualização da Custódia Master

### 7.1. Para vendas:
- Remover quantidades vendidas da custodia master
- Se foi venda total de um ativo, zerar saldo master desse ativo

### 7.2. Para compras:
- Adicionar à custodia master primeiro
- Depois distribuir para filhotes (mesma lógica da compra programada)
- Residuos ficam na master

---

## ETAPA 8: Histórico e Auditoria

### 8.1. Registrar todas as operações:
- Tabela de histórico de operações ou ordens
- Campos sugeridos:
  - cliente_id
  - ticker
  - tipo_operacao (COMPRA/VENDA)
  - tipo_ordem (REBALANCEAMENTO)
  - quantidade
  - preço_unitário
  - valor_total
  - data_execução

### 8.2. Atualizar conta gráfica:
- data_ultima_atualizacao = agora

---

## ETAPA 9: Mensagens Kafka

### 9.1. Tópico `ir-dedo-duro`:
```json
{
  "tipo": "IR_DEDO_DURO",
  "clienteId": 1,
  "cpf": "12345678901",
  "ticker": "PETR4",
  "tipoOperacao": "COMPRA" ou "VENDA",
  "quantidade": 8,
  "precoUnitario": 35.00,
  "valorOperacao": 280.00,
  "aliquota": 0.00005,
  "valorIR": 0.01,
  "dataOperacao": "2026-02-05T10:00:00Z"
}
```

### 9.2. Tópico `ir-vendas`:
```json
{
  "tipo": "IR_VENDA",
  "clienteId": 2,
  "cpf": "98765432109",
  "mesReferencia": "2026-03",
  "totalVendasMes": 21500.00,
  "lucroLiquido": 3100.00,
  "aliquota": 0.20,
  "valorIR": 620.00,
  "detalhes": [
    {
      "ticker": "BBDC4",
      "quantidade": 500,
      "precoVenda": 16.00,
      "precoMedio": 14.00,
      "lucro": 1000.00
    }
  ],
  "dataCalculo": "2026-03-15T10:00:00Z"
}
```

---

## ETAPA 10: Processamento Assíncrono (Recomendado)

Como o rebalanceamento afeta **TODOS os clientes ativos**, é recomendado:

### 10.1. Publicar evento no Kafka:
- Tópico: `rebalanceamento-cesta`
- Payload:
  ```json
  {
    "cestaAnteriorId": 1,
    "cestaNovaId": 2,
    "dataExecucao": "2026-03-01T10:00:00Z"
  }
  ```

### 10.2. Consumer processar em lote:
- Buscar todos os clientes ativos
- Para cada cliente, executar ETAPAS 3-7
- Pode processar em paralelo (thread pool)
- Implementar retry logic

---

## RESUMO DOS PRINCIPAIS PONTOS DE ATENÇÃO

✅ **Ordem das operações:** Vender primeiro, depois comprar  
✅ **Preço médio:** Recalcula na compra, mantém na venda  
✅ **IR Dedo-Duro:** Toda operação (compra E venda)  
✅ **IR Vendas:** Só se total vendas mês > R$ 20k E lucro > 0  
✅ **Lote padrão vs fracionário:** >= 100 = lote, < 100 = fracionário (sufixo F)  
✅ **Residuos:** Ficam na custodia master  
✅ **Truncamento:** Sempre arredondar para baixo nas quantidades  
✅ **Proporção:** Baseada no valor atual da carteira do cliente  
✅ **Kafka:** Publish não-bloqueante, garantir entrega  
✅ **Atomicidade:** Usar transações para garantir consistência  

---

## Divisão de Implementação

A implementação pode ser dividida em:

1. **Service de rebalanceamento** (lógica principal)
2. **Producers Kafka** (IR dedo-duro e IR vendas)
3. **Consumer Kafka** (processar rebalanceamento assíncrono)
4. **Controller** (endpoint admin)

---

*Este documento é parte do material de apoio do Desafio Técnico de Compra Programada de Ações da Itaú Corretora.*

