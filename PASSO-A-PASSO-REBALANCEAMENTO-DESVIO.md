# 📊 PASSO A PASSO - REBALANCEAMENTO POR DESVIO DE PROPORÇÃO

## 🎯 Objetivo

Criar um endpoint no Motor de Compras que verifique **todos os clientes ativos** e identifique aqueles cuja carteira está com proporções desviadas em relação à cesta Top Five atual, executando o rebalanceamento automático.

---

## 📋 Conceito

O **Rebalanceamento por Desvio** ocorre quando a valorização ou desvalorização natural dos ativos causa uma divergência significativa entre:
- **Proporção Atual** da carteira do cliente
- **Proporção Alvo** definida na cesta Top Five

**Exemplo:**
```
Cesta Top Five (Alvo):
- PETR4: 30%
- VALE3: 25%
- ITUB4: 20%
- BBDC4: 15%
- ABEV3: 10%

Carteira Real do Cliente (após valorização):
- PETR4: 45% ⚠️ DESVIOU +15 pontos
- VALE3: 20% ✅ OK (-5 pontos)
- ITUB4: 18% ✅ OK (-2 pontos)
- BBDC4: 10% ⚠️ DESVIOU -5 pontos
- ABEV3: 7%  ⚠️ DESVIOU -3 pontos

Ação: Vender excesso de PETR4 e comprar BBDC4 e ABEV3
```

---

## ⚙️ CONFIGURAÇÃO DO LIMIAR DE DESVIO

**Regra RN-051:** Definir limiar de desvio (sugestão: **5 pontos percentuais**)

### Opções de Configuração:

#### Opção 1: Configuração via application.yaml (Recomendado)
```yaml
# application.yaml
rebalanceamento:
  desvio:
    limiar-percentual: 5.0  # Desvio mínimo para disparar rebalanceamento
    ativo: true              # Se o rebalanceamento por desvio está ativo
```

#### Opção 2: Constante na aplicação
```java
public class RebalanceamentoConfig {
    public static final BigDecimal LIMIAR_DESVIO_PERCENTUAL = new BigDecimal("5.00");
}
```

---

## 🔍 PASSO 1: Criar Endpoint de Verificação

### 1.1 Controller

**Endpoint:** `POST /api/motor/rebalanceamento/verificar-desvio`

```java
@RestController
@RequestMapping("/api/motor/rebalanceamento")
@RequiredArgsConstructor
@Slf4j
public class RebalanceamentoDesvioController {

    private final RebalanceamentoDesvioService rebalanceamentoDesvioService;

    @PostMapping("/verificar-desvio")
    @Operation(
        summary = "Verificar e rebalancear carteiras com desvio de proporção",
        description = "Verifica todos os clientes ativos e rebalanceia aqueles cuja carteira " +
                      "apresenta desvio superior ao limiar configurado em relação à cesta Top Five"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Verificação concluída",
            content = @Content(schema = @Schema(implementation = RebalanceamentoDesvioResponseDTO.class))
        )
    })
    public ResponseEntity<RebalanceamentoDesvioResponseDTO> verificarERebalancear() {
        log.info("Iniciando verificação de desvio de proporção para todos os clientes");
        RebalanceamentoDesvioResponseDTO response = rebalanceamentoDesvioService.verificarERebalancearTodos();
        return ResponseEntity.ok(response);
    }
}
```

### 1.2 DTO de Response

```java
@Data
@Builder
public class RebalanceamentoDesvioResponseDTO {
    private Integer totalClientesVerificados;
    private Integer clientesComDesvio;
    private Integer clientesRebalanceados;
    private BigDecimal limiarUtilizado;
    private List<ClienteRebalanceadoDTO> detalhes;
    private LocalDateTime dataExecucao;
}

@Data
@Builder
public class ClienteRebalanceadoDTO {
    private Long clienteId;
    private String nomeCliente;
    private String cpf;
    private BigDecimal valorCarteira;
    private List<DesvioAtivoDTO> desviosDetectados;
    private List<OperacaoRebalanceamentoDTO> operacoesExecutadas;
    private BigDecimal valorTotalVendido;
    private BigDecimal valorTotalComprado;
    private BigDecimal irCalculado;
}

@Data
@Builder
public class DesvioAtivoDTO {
    private String ticker;
    private BigDecimal percentualAtual;
    private BigDecimal percentualAlvo;
    private BigDecimal desvio;  // Diferença em pontos percentuais
    private String tipoDesvio;  // "SOBRE_ALOCADO" ou "SUB_ALOCADO"
}

@Data
@Builder
public class OperacaoRebalanceamentoDTO {
    private String tipo; // "VENDA" ou "COMPRA"
    private String ticker;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal valorTotal;
}
```

---

## 🧮 PASSO 2: Implementar Lógica de Verificação

### 2.1 Service Principal

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RebalanceamentoDesvioService {

    private final ClienteFeignClient clienteFeignClient;
    private final CestaFeignClient cestaFeignClient;
    private final CustodiaService custodiaService;
    private final CotacaoFeignClient cotacaoFeignClient;
    private final RebalanceamentoExecutorService rebalanceamentoExecutorService;
    
    @Value("${rebalanceamento.desvio.limiar-percentual:5.0}")
    private BigDecimal limiarDesvio;
    
    public RebalanceamentoDesvioResponseDTO verificarERebalancearTodos() {
        LocalDateTime inicioExecucao = LocalDateTime.now();
        
        // 1. Buscar cesta ativa
        CestaResponseDTO cestaAtiva = cestaFeignClient.obterCestaAtual();
        if (cestaAtiva == null) {
            throw new NegocioException("Nenhuma cesta ativa encontrada");
        }
        
        // 2. Buscar todos os clientes ativos
        List<ClienteResponseDTO> clientesAtivos = clienteFeignClient.buscarClientesAtivos();
        
        List<ClienteRebalanceadoDTO> clientesRebalanceados = new ArrayList<>();
        int clientesComDesvio = 0;
        int clientesRebalanceadosCount = 0;
        
        // 3. Para cada cliente, verificar desvio
        for (ClienteResponseDTO cliente : clientesAtivos) {
            try {
                AnaliseDesvioDTO analise = analisarDesvioCliente(cliente.getId(), cestaAtiva);
                
                if (analise.isPossuiDesvio()) {
                    clientesComDesvio++;
                    
                    log.info("Cliente {} possui desvio. Iniciando rebalanceamento...", cliente.getId());
                    
                    // 4. Executar rebalanceamento
                    ClienteRebalanceadoDTO resultado = rebalanceamentoExecutorService
                        .executarRebalanceamentoPorDesvio(cliente, cestaAtiva, analise);
                    
                    clientesRebalanceados.add(resultado);
                    clientesRebalanceadosCount++;
                    
                    log.info("Rebalanceamento do cliente {} concluído com sucesso", cliente.getId());
                }
            } catch (Exception e) {
                log.error("Erro ao processar cliente {}: {}", cliente.getId(), e.getMessage(), e);
                // Continuar processando os demais clientes
            }
        }
        
        return RebalanceamentoDesvioResponseDTO.builder()
            .totalClientesVerificados(clientesAtivos.size())
            .clientesComDesvio(clientesComDesvio)
            .clientesRebalanceados(clientesRebalanceadosCount)
            .limiarUtilizado(limiarDesvio)
            .detalhes(clientesRebalanceados)
            .dataExecucao(inicioExecucao)
            .build();
    }
    
    private AnaliseDesvioDTO analisarDesvioCliente(Long clienteId, CestaResponseDTO cestaAtiva) {
        // Implementação no PASSO 3
    }
}
```

---

## 🔍 PASSO 3: Analisar Desvio de Proporção

### 3.1 Calcular Proporções Atuais da Carteira

```java
private AnaliseDesvioDTO analisarDesvioCliente(Long clienteId, CestaResponseDTO cestaAtiva) {
    
    // 1. Buscar custódia do cliente
    List<CustodiaResponseDTO> custodias = custodiaService.consultarCustodiasCliente(clienteId);
    
    if (custodias.isEmpty()) {
        return AnaliseDesvioDTO.semDesvio();
    }
    
    // 2. Obter cotações atuais de todos os ativos
    Map<String, BigDecimal> cotacoesAtuais = obterCotacoesAtuais(custodias);
    
    // 3. Calcular valor total da carteira
    BigDecimal valorTotalCarteira = calcularValorTotalCarteira(custodias, cotacoesAtuais);
    
    if (valorTotalCarteira.compareTo(BigDecimal.ZERO) == 0) {
        return AnaliseDesvioDTO.semDesvio();
    }
    
    // 4. Calcular proporção atual de cada ativo
    Map<String, BigDecimal> proporcoesAtuais = new HashMap<>();
    for (CustodiaResponseDTO custodia : custodias) {
        BigDecimal cotacao = cotacoesAtuais.get(custodia.getTicker());
        BigDecimal valorAtivo = BigDecimal.valueOf(custodia.getQuantidade())
            .multiply(cotacao);
        
        BigDecimal proporcao = valorAtivo
            .divide(valorTotalCarteira, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100")); // Converter para percentual
        
        proporcoesAtuais.put(custodia.getTicker(), proporcao);
    }
    
    // 5. Comparar com proporções da cesta (alvo)
    Map<String, BigDecimal> proporcoesAlvo = cestaAtiva.getItens().stream()
        .collect(Collectors.toMap(
            ItemCestaDTO::getTicker,
            ItemCestaDTO::getPercentual
        ));
    
    // 6. Calcular desvios
    List<DesvioAtivoDTO> desvios = new ArrayList<>();
    boolean possuiDesvio = false;
    
    for (ItemCestaDTO item : cestaAtiva.getItens()) {
        String ticker = item.getTicker();
        BigDecimal percentualAlvo = item.getPercentual();
        BigDecimal percentualAtual = proporcoesAtuais.getOrDefault(ticker, BigDecimal.ZERO);
        
        BigDecimal desvio = percentualAtual.subtract(percentualAlvo);
        BigDecimal desvioAbsoluto = desvio.abs();
        
        // Verificar se desvio ultrapassa o limiar
        if (desvioAbsoluto.compareTo(limiarDesvio) > 0) {
            possuiDesvio = true;
            
            String tipoDesvio = desvio.compareTo(BigDecimal.ZERO) > 0 
                ? "SOBRE_ALOCADO" 
                : "SUB_ALOCADO";
            
            desvios.add(DesvioAtivoDTO.builder()
                .ticker(ticker)
                .percentualAtual(percentualAtual)
                .percentualAlvo(percentualAlvo)
                .desvio(desvio)
                .tipoDesvio(tipoDesvio)
                .build());
        }
    }
    
    return AnaliseDesvioDTO.builder()
        .possuiDesvio(possuiDesvio)
        .valorTotalCarteira(valorTotalCarteira)
        .proporcoesAtuais(proporcoesAtuais)
        .proporcoesAlvo(proporcoesAlvo)
        .desvios(desvios)
        .cotacoesAtuais(cotacoesAtuais)
        .build();
}

private Map<String, BigDecimal> obterCotacoesAtuais(List<CustodiaResponseDTO> custodias) {
    Map<String, BigDecimal> cotacoes = new HashMap<>();
    
    for (CustodiaResponseDTO custodia : custodias) {
        CotacaoResponseDTO cotacao = cotacaoFeignClient.obterUltimaCotacao(custodia.getTicker());
        if (cotacao != null) {
            cotacoes.put(custodia.getTicker(), cotacao.getPrecoFechamento());
        } else {
            log.warn("Cotação não encontrada para ticker: {}", custodia.getTicker());
            cotacoes.put(custodia.getTicker(), BigDecimal.ZERO);
        }
    }
    
    return cotacoes;
}

private BigDecimal calcularValorTotalCarteira(
        List<CustodiaResponseDTO> custodias, 
        Map<String, BigDecimal> cotacoes) {
    
    return custodias.stream()
        .map(custodia -> {
            BigDecimal cotacao = cotacoes.getOrDefault(custodia.getTicker(), BigDecimal.ZERO);
            return BigDecimal.valueOf(custodia.getQuantidade()).multiply(cotacao);
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

### 3.2 DTO de Análise

```java
@Data
@Builder
public class AnaliseDesvioDTO {
    private boolean possuiDesvio;
    private BigDecimal valorTotalCarteira;
    private Map<String, BigDecimal> proporcoesAtuais;
    private Map<String, BigDecimal> proporcoesAlvo;
    private List<DesvioAtivoDTO> desvios;
    private Map<String, BigDecimal> cotacoesAtuais;
    
    public static AnaliseDesvioDTO semDesvio() {
        return AnaliseDesvioDTO.builder()
            .possuiDesvio(false)
            .build();
    }
}
```

---

## 💰 PASSO 4: Executar Rebalanceamento

### 4.1 Service de Execução

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RebalanceamentoExecutorService {

    private final CustodiaService custodiaService;
    private final OrdemCompraRepository ordemCompraRepository;
    private final DistribuicaoRepository distribuicaoRepository;
    private final RebalanceamentoRepository rebalanceamentoRepository;
    private final IrProducerService irProducerService;
    
    public ClienteRebalanceadoDTO executarRebalanceamentoPorDesvio(
            ClienteResponseDTO cliente,
            CestaResponseDTO cestaAtiva,
            AnaliseDesvioDTO analise) {
        
        List<OperacaoRebalanceamentoDTO> operacoes = new ArrayList<>();
        BigDecimal valorTotalVendido = BigDecimal.ZERO;
        BigDecimal valorTotalComprado = BigDecimal.ZERO;
        
        // 1. VENDER ativos SOBRE-ALOCADOS
        for (DesvioAtivoDTO desvio : analise.getDesvios()) {
            if ("SOBRE_ALOCADO".equals(desvio.getTipoDesvio())) {
                OperacaoRebalanceamentoDTO vendaOp = venderExcesso(
                    cliente.getId(), 
                    desvio, 
                    analise
                );
                operacoes.add(vendaOp);
                valorTotalVendido = valorTotalVendido.add(vendaOp.getValorTotal());
            }
        }
        
        // 2. COMPRAR ativos SUB-ALOCADOS
        for (DesvioAtivoDTO desvio : analise.getDesvios()) {
            if ("SUB_ALOCADO".equals(desvio.getTipoDesvio())) {
                OperacaoRebalanceamentoDTO compraOp = comprarDeficit(
                    cliente.getId(), 
                    desvio, 
                    analise,
                    valorTotalVendido  // Usar o dinheiro das vendas
                );
                operacoes.add(compraOp);
                valorTotalComprado = valorTotalComprado.add(compraOp.getValorTotal());
            }
        }
        
        // 3. Calcular IR
        BigDecimal irTotal = calcularIRRebalanceamento(
            cliente, 
            operacoes, 
            valorTotalVendido
        );
        
        // 4. Registrar rebalanceamento no histórico
        registrarRebalanceamento(cliente.getId(), operacoes, "DESVIO");
        
        return ClienteRebalanceadoDTO.builder()
            .clienteId(cliente.getId())
            .nomeCliente(cliente.getNome())
            .cpf(cliente.getCpf())
            .valorCarteira(analise.getValorTotalCarteira())
            .desviosDetectados(analise.getDesvios())
            .operacoesExecutadas(operacoes)
            .valorTotalVendido(valorTotalVendido)
            .valorTotalComprado(valorTotalComprado)
            .irCalculado(irTotal)
            .build();
    }
    
    // Continua nos próximos métodos...
}
```

---

## 📉 PASSO 5: Vender Ativos Sobre-Alocados

```java
private OperacaoRebalanceamentoDTO venderExcesso(
        Long clienteId,
        DesvioAtivoDTO desvio,
        AnaliseDesvioDTO analise) {
    
    String ticker = desvio.getTicker();
    
    // 1. Buscar custódia atual do cliente para este ativo
    CustodiaResponseDTO custodia = custodiaService.buscarCustodiaPorTicker(clienteId, ticker);
    
    if (custodia == null || custodia.getQuantidade() == 0) {
        log.warn("Cliente {} não possui {} em custódia", clienteId, ticker);
        return null;
    }
    
    // 2. Calcular quantidade alvo (ideal) deste ativo
    BigDecimal percentualAlvo = desvio.getPercentualAlvo();
    BigDecimal valorAlvo = analise.getValorTotalCarteira()
        .multiply(percentualAlvo)
        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    
    BigDecimal cotacaoAtual = analise.getCotacoesAtuais().get(ticker);
    
    int quantidadeAlvo = valorAlvo
        .divide(cotacaoAtual, 0, RoundingMode.DOWN)
        .intValue();
    
    // 3. Calcular quantas ações vender
    int quantidadeAtual = custodia.getQuantidade();
    int quantidadeAVender = quantidadeAtual - quantidadeAlvo;
    
    if (quantidadeAVender <= 0) {
        log.warn("Quantidade a vender é zero ou negativa para {}", ticker);
        return null;
    }
    
    // 4. Executar venda
    BigDecimal valorVenda = cotacaoAtual.multiply(new BigDecimal(quantidadeAVender));
    
    // 5. Atualizar custódia
    custodiaService.atualizarCustodia(
        clienteId,
        ticker,
        quantidadeAtual - quantidadeAVender,
        custodia.getPrecoMedio() // Preço médio NÃO muda na venda
    );
    
    // 6. Calcular lucro/prejuízo
    BigDecimal lucro = cotacaoAtual
        .subtract(custodia.getPrecoMedio())
        .multiply(new BigDecimal(quantidadeAVender));
    
    // 7. Publicar IR dedo-duro
    irProducerService.publicarIrDedoDuro(
        clienteId,
        ticker,
        "VENDA",
        quantidadeAVender,
        cotacaoAtual,
        valorVenda
    );
    
    log.info("Vendido {} unidades de {} para cliente {} por R$ {}", 
        quantidadeAVender, ticker, clienteId, valorVenda);
    
    return OperacaoRebalanceamentoDTO.builder()
        .tipo("VENDA")
        .ticker(ticker)
        .quantidade(quantidadeAVender)
        .precoUnitario(cotacaoAtual)
        .valorTotal(valorVenda)
        .build();
}
```

---

## 📈 PASSO 6: Comprar Ativos Sub-Alocados

```java
private OperacaoRebalanceamentoDTO comprarDeficit(
        Long clienteId,
        DesvioAtivoDTO desvio,
        AnaliseDesvioDTO analise,
        BigDecimal saldoDisponivel) {
    
    String ticker = desvio.getTicker();
    
    // 1. Calcular valor alvo deste ativo
    BigDecimal percentualAlvo = desvio.getPercentualAlvo();
    BigDecimal valorAlvo = analise.getValorTotalCarteira()
        .multiply(percentualAlvo)
        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    
    BigDecimal cotacaoAtual = analise.getCotacoesAtuais().get(ticker);
    
    // 2. Calcular quantidade alvo
    int quantidadeAlvo = valorAlvo
        .divide(cotacaoAtual, 0, RoundingMode.DOWN)
        .intValue();
    
    // 3. Buscar custódia atual (pode não existir)
    CustodiaResponseDTO custodia = custodiaService.buscarCustodiaPorTicker(clienteId, ticker);
    int quantidadeAtual = custodia != null ? custodia.getQuantidade() : 0;
    
    // 4. Calcular quantas ações comprar
    int quantidadeAComprar = quantidadeAlvo - quantidadeAtual;
    
    if (quantidadeAComprar <= 0) {
        log.warn("Quantidade a comprar é zero ou negativa para {}", ticker);
        return null;
    }
    
    // 5. Verificar se há saldo disponível
    BigDecimal valorCompra = cotacaoAtual.multiply(new BigDecimal(quantidadeAComprar));
    
    if (valorCompra.compareTo(saldoDisponivel) > 0) {
        // Ajustar quantidade para o saldo disponível
        quantidadeAComprar = saldoDisponivel
            .divide(cotacaoAtual, 0, RoundingMode.DOWN)
            .intValue();
        
        if (quantidadeAComprar <= 0) {
            log.warn("Saldo insuficiente para comprar {}", ticker);
            return null;
        }
        
        valorCompra = cotacaoAtual.multiply(new BigDecimal(quantidadeAComprar));
    }
    
    // 6. Executar compra e atualizar custódia com novo preço médio
    BigDecimal novoPrecoMedio;
    
    if (quantidadeAtual > 0) {
        // Recalcular preço médio
        BigDecimal valorAnterior = custodia.getPrecoMedio()
            .multiply(new BigDecimal(quantidadeAtual));
        BigDecimal valorNovo = cotacaoAtual
            .multiply(new BigDecimal(quantidadeAComprar));
        
        novoPrecoMedio = valorAnterior.add(valorNovo)
            .divide(new BigDecimal(quantidadeAtual + quantidadeAComprar), 4, RoundingMode.HALF_UP);
    } else {
        // Primeira compra
        novoPrecoMedio = cotacaoAtual;
    }
    
    // 7. Atualizar custódia
    custodiaService.atualizarCustodia(
        clienteId,
        ticker,
        quantidadeAtual + quantidadeAComprar,
        novoPrecoMedio
    );
    
    // 8. Publicar IR dedo-duro
    irProducerService.publicarIrDedoDuro(
        clienteId,
        ticker,
        "COMPRA",
        quantidadeAComprar,
        cotacaoAtual,
        valorCompra
    );
    
    log.info("Comprado {} unidades de {} para cliente {} por R$ {}", 
        quantidadeAComprar, ticker, clienteId, valorCompra);
    
    return OperacaoRebalanceamentoDTO.builder()
        .tipo("COMPRA")
        .ticker(ticker)
        .quantidade(quantidadeAComprar)
        .precoUnitario(cotacaoAtual)
        .valorTotal(valorCompra)
        .build();
}
```

---

## 💸 PASSO 7: Calcular IR sobre Vendas

```java
private BigDecimal calcularIRRebalanceamento(
        ClienteResponseDTO cliente,
        List<OperacaoRebalanceamentoDTO> operacoes,
        BigDecimal valorTotalVendido) {
    
    // Regra: IR de 20% sobre lucro apenas se vendas > R$ 20.000 no mês
    
    // 1. Buscar total de vendas do cliente no mês atual
    LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
    LocalDate fimMes = LocalDate.now().withDayOfMonth(
        LocalDate.now().lengthOfMonth()
    );
    
    BigDecimal vendasMesAtual = rebalanceamentoRepository
        .calcularTotalVendasMes(cliente.getId(), inicioMes, fimMes);
    
    BigDecimal totalVendas = vendasMesAtual.add(valorTotalVendido);
    
    // 2. Verificar se ultrapassa R$ 20.000
    BigDecimal LIMITE_ISENCAO = new BigDecimal("20000.00");
    
    if (totalVendas.compareTo(LIMITE_ISENCAO) <= 0) {
        log.info("Cliente {} isento de IR sobre vendas (total: R$ {})", 
            cliente.getId(), totalVendas);
        return BigDecimal.ZERO;
    }
    
    // 3. Calcular lucro líquido das vendas
    BigDecimal lucroTotal = BigDecimal.ZERO;
    
    for (OperacaoRebalanceamentoDTO op : operacoes) {
        if ("VENDA".equals(op.getTipo())) {
            // Buscar preço médio da custódia
            CustodiaResponseDTO custodia = custodiaService
                .buscarCustodiaPorTicker(cliente.getId(), op.getTicker());
            
            if (custodia != null) {
                BigDecimal precoMedio = custodia.getPrecoMedio();
                BigDecimal lucroOperacao = op.getPrecoUnitario()
                    .subtract(precoMedio)
                    .multiply(new BigDecimal(op.getQuantidade()));
                
                lucroTotal = lucroTotal.add(lucroOperacao);
            }
        }
    }
    
    // 4. Calcular IR apenas se houver lucro
    if (lucroTotal.compareTo(BigDecimal.ZERO) <= 0) {
        log.info("Cliente {} não possui lucro tributável (prejuízo: R$ {})", 
            cliente.getId(), lucroTotal);
        return BigDecimal.ZERO;
    }
    
    // 5. IR = 20% sobre o lucro
    BigDecimal ALIQUOTA_IR = new BigDecimal("0.20");
    BigDecimal irDevido = lucroTotal.multiply(ALIQUOTA_IR);
    
    // 6. Publicar evento de IR sobre vendas no Kafka
    irProducerService.publicarIrVendas(
        cliente.getId(),
        cliente.getCpf(),
        LocalDate.now().getMonth().toString(),
        totalVendas,
        lucroTotal,
        irDevido,
        operacoes
    );
    
    log.info("IR sobre vendas calculado para cliente {}: R$ {}", 
        cliente.getId(), irDevido);
    
    return irDevido;
}
```

---

## 📝 PASSO 8: Registrar Histórico

```java
private void registrarRebalanceamento(
        Long clienteId, 
        List<OperacaoRebalanceamentoDTO> operacoes,
        String tipo) {
    
    for (OperacaoRebalanceamentoDTO op : operacoes) {
        Rebalanceamento rebalanceamento = Rebalanceamento.builder()
            .clienteId(clienteId)
            .tipo(tipo) // "DESVIO"
            .tickerVendido("VENDA".equals(op.getTipo()) ? op.getTicker() : null)
            .tickerComprado("COMPRA".equals(op.getTipo()) ? op.getTicker() : null)
            .valorVenda("VENDA".equals(op.getTipo()) ? op.getValorTotal() : null)
            .dataRebalanceamento(LocalDateTime.now())
            .build();
        
        rebalanceamentoRepository.save(rebalanceamento);
    }
}
```

---

## 🔧 PASSO 9: Configurações Adicionais

### 9.1 Adicionar no application.yaml

```yaml
# Configuração do Rebalanceamento por Desvio
rebalanceamento:
  desvio:
    limiar-percentual: 5.0  # Desvio mínimo em pontos percentuais
    ativo: true             # Habilitar/desabilitar funcionalidade
    
# Logs específicos
logging:
  level:
    com.itau.srv.motor.compras.service.RebalanceamentoDesvioService: DEBUG
    com.itau.srv.motor.compras.service.RebalanceamentoExecutorService: DEBUG
```

### 9.2 Documentação Swagger

Adicionar tags e descrições completas nos controllers para documentação automática.

---

## 🎯 PASSO 10: Testes

### 10.1 Teste Unitário - Análise de Desvio

```java
@Test
void deveDetectarDesvioQuandoSuperaLimiar() {
    // Given
    Long clienteId = 1L;
    BigDecimal limiar = new BigDecimal("5.0");
    
    CestaResponseDTO cesta = criarCesta();
    List<CustodiaResponseDTO> custodias = criarCustodiasComDesvio();
    
    // Mock das cotações
    when(cotacaoFeignClient.obterUltimaCotacao(anyString()))
        .thenReturn(criarCotacao());
    
    // When
    AnaliseDesvioDTO analise = service.analisarDesvioCliente(clienteId, cesta);
    
    // Then
    assertTrue(analise.isPossuiDesvio());
    assertFalse(analise.getDesvios().isEmpty());
    assertTrue(analise.getDesvios().get(0).getDesvio().abs()
        .compareTo(limiar) > 0);
}
```

### 10.2 Teste de Integração

```java
@Test
@Transactional
void deveRebalancearCarteiraComDesvio() {
    // Given
    ClienteResponseDTO cliente = criarCliente();
    criarCestaAtiva();
    criarCustodiaComDesvio(cliente.getId());
    
    // When
    RebalanceamentoDesvioResponseDTO response = service.verificarERebalancearTodos();
    
    // Then
    assertEquals(1, response.getClientesRebalanceados());
    assertFalse(response.getDetalhes().isEmpty());
    
    // Verificar se operações foram registradas
    List<Rebalanceamento> historico = rebalanceamentoRepository
        .findByClienteId(cliente.getId());
    assertFalse(historico.isEmpty());
}
```

---

## 📊 PASSO 11: Exemplo de Fluxo Completo

### Cenário de Teste:

```
Cliente: João Silva (ID: 1)
Valor da Carteira: R$ 10.000,00
Limiar de Desvio: 5 pontos percentuais

Cesta Top Five (Alvo):
- PETR4: 30%
- VALE3: 25%
- ITUB4: 20%
- BBDC4: 15%
- ABEV3: 10%

Carteira Atual (após valorização de PETR4):
- PETR4: 50 ações x R$ 90,00 = R$ 4.500 (45%) ⚠️ +15 pontos
- VALE3: 40 ações x R$ 70,00 = R$ 2.800 (28%) ⚠️ +3 pontos (OK)
- ITUB4: 70 ações x R$ 30,00 = R$ 2.100 (21%) ✅ +1 ponto (OK)
- BBDC4: 30 ações x R$ 20,00 = R$   600 (6%)  ⚠️ -9 pontos
- ABEV3: 10 ações x R$ 10,00 = R$   100 (1%)  ⚠️ -9 pontos

Execução do Rebalanceamento:

1. Análise:
   - PETR4 está SOBRE_ALOCADO (+15 pontos)
   - BBDC4 está SUB_ALOCADO (-9 pontos)
   - ABEV3 está SUB_ALOCADO (-9 pontos)

2. Vender Excesso de PETR4:
   - Valor alvo: R$ 10.000 x 30% = R$ 3.000
   - Quantidade alvo: R$ 3.000 / R$ 90 = 33 ações
   - Quantidade atual: 50 ações
   - VENDER: 50 - 33 = 17 ações
   - Valor obtido: 17 x R$ 90 = R$ 1.530

3. Comprar BBDC4:
   - Valor alvo: R$ 10.000 x 15% = R$ 1.500
   - Quantidade alvo: R$ 1.500 / R$ 20 = 75 ações
   - Quantidade atual: 30 ações
   - COMPRAR: 75 - 30 = 45 ações
   - Valor necessário: 45 x R$ 20 = R$ 900

4. Comprar ABEV3:
   - Valor alvo: R$ 10.000 x 10% = R$ 1.000
   - Quantidade alvo: R$ 1.000 / R$ 10 = 100 ações
   - Quantidade atual: 10 ações
   - COMPRAR: 100 - 10 = 90 ações
   - Valor necessário: 90 x R$ 10 = R$ 900
   - ⚠️ Saldo disponível: R$ 1.530 - R$ 900 = R$ 630
   - COMPRAR: R$ 630 / R$ 10 = 63 ações

5. Resultado Final:
   - Total vendido: R$ 1.530
   - Total comprado: R$ 1.530
   - IR dedo-duro publicado: 3 eventos no Kafka
   - Carteira rebalanceada ✅
```

---

## 🚀 PASSO 12: Automação (Opcional)

### 12.1 Scheduled Task

Para executar verificação automática periodicamente:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class RebalanceamentoDesvioScheduler {

    private final RebalanceamentoDesvioService rebalanceamentoDesvioService;
    
    @Value("${rebalanceamento.desvio.ativo:false}")
    private boolean rebalanceamentoAtivo;
    
    // Executar todos os dias às 18h (após fechamento do mercado)
    @Scheduled(cron = "0 0 18 * * MON-FRI")
    public void verificarDesvioAutomatico() {
        if (!rebalanceamentoAtivo) {
            log.info("Rebalanceamento por desvio está desativado");
            return;
        }
        
        log.info("Iniciando verificação automática de desvio de proporção");
        
        try {
            RebalanceamentoDesvioResponseDTO resultado = 
                rebalanceamentoDesvioService.verificarERebalancearTodos();
            
            log.info("Verificação concluída: {} clientes verificados, {} rebalanceados",
                resultado.getTotalClientesVerificados(),
                resultado.getClientesRebalanceados());
                
        } catch (Exception e) {
            log.error("Erro ao executar verificação automática de desvio", e);
        }
    }
}
```

---

## ✅ CHECKLIST DE IMPLEMENTAÇÃO

- [ ] Controller com endpoint `/api/motor/rebalanceamento/verificar-desvio`
- [ ] DTOs de request/response criados
- [ ] Service de análise de desvio implementado
- [ ] Lógica de cálculo de proporções
- [ ] Comparação com limiar configurável
- [ ] Service executor de rebalanceamento
- [ ] Venda de ativos sobre-alocados
- [ ] Compra de ativos sub-alocados
- [ ] Atualização de custódias com preço médio
- [ ] Cálculo de IR (dedo-duro e vendas)
- [ ] Publicação de eventos no Kafka
- [ ] Registro no histórico de rebalanceamentos
- [ ] Configurações no application.yaml
- [ ] Documentação Swagger
- [ ] Testes unitários
- [ ] Testes de integração
- [ ] (Opcional) Scheduled task automático
- [ ] Logs estruturados
- [ ] Tratamento de erros

---

## 🎯 RESUMO DOS PRINCIPAIS PONTOS

### Lógica Core:

1. **Buscar clientes ativos** → Para cada um:
2. **Calcular proporção atual** da carteira (valor de cada ativo / valor total)
3. **Comparar com proporção alvo** da cesta Top Five
4. **Identificar desvios** > limiar (ex: 5 pontos percentuais)
5. **Vender** ativos sobre-alocados (excesso)
6. **Comprar** ativos sub-alocados (deficit) com o dinheiro das vendas
7. **Atualizar preços médios** (na compra) e **manter** (na venda)
8. **Publicar IR** dedo-duro e sobre vendas (se > R$ 20k)
9. **Registrar** no histórico de rebalanceamentos

### Diferenças do Rebalanceamento por Mudança de Cesta:

| Aspecto | Por Mudança de Cesta | Por Desvio |
|---------|---------------------|------------|
| **Trigger** | Admin altera cesta | Mercado valoriza/desvaloriza ativos |
| **Frequência** | Sob demanda | Periódica (diária, semanal) |
| **Critério** | Ativos que entraram/saíram | Desvio > limiar (5%) |
| **Escopo** | Todos os clientes | Apenas clientes com desvio |
| **Vendas** | 100% de ativos removidos | Apenas excesso |

---

**Boa implementação! 🚀**

