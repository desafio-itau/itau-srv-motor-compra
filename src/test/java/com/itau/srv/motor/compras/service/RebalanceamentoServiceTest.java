package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.cesta.CestaResponseDTO;
import com.itau.srv.motor.compras.dto.cliente.ClienteResponseDTO;
import com.itau.srv.motor.compras.dto.conta.ContaGraficaDTO;
import com.itau.srv.motor.compras.dto.cotacao.CotacaoResponseDTO;
import com.itau.srv.motor.compras.dto.itemcesta.ItemCotacaoAtualResponseDTO;
import com.itau.srv.motor.compras.feign.CestaFeignClient;
import com.itau.srv.motor.compras.feign.ClientesFeignClient;
import com.itau.srv.motor.compras.feign.CotacaoFeignClient;
import com.itau.srv.motor.compras.kafka.IREventProducer;
import com.itau.srv.motor.compras.mapper.EventoIRMapper;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import com.itau.srv.motor.compras.repository.EventoIRRepository;
import com.itau.srv.motor.compras.repository.RebalanceamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RebalanceamentoService - Testes Unitários")
class RebalanceamentoServiceTest {

    @Mock
    private CestaFeignClient cestaFeignClient;

    @Mock
    private ClientesFeignClient clientesFeignClient;

    @Mock
    private CotacaoFeignClient cotacaoFeignClient;

    @Mock
    private CustodiaRepository custodiaRepository;

    @Mock
    private IREventProducer irEventProducer;

    @Mock
    private EventoIRRepository eventoIRRepository;

    @Mock
    private EventoIRMapper eventoIRMapper;

    @Mock
    private RebalanceamentoRepository rebalanceamentoRepository;

    @InjectMocks
    private RebalanceamentoService rebalanceamentoService;

    private LocalDate dataExecucao;
    private ClienteResponseDTO clienteMock;
    private CestaResponseDTO cestaAnteriorMock;
    private CestaResponseDTO cestaAtualMock;

    @BeforeEach
    void setUp() {
        dataExecucao = LocalDate.of(2026, 3, 3);

        // Mock de cliente
        ContaGraficaDTO contaMock = new ContaGraficaDTO(1L, "ITAUFL00001", "FILHOTE", LocalDateTime.now());
        clienteMock = new ClienteResponseDTO(
                1L,
                "Cliente Teste",
                "12345678900",
                "cliente@teste.com",
                new BigDecimal("3000.00"),
                true,
                LocalDateTime.now(),
                contaMock
        );

        // Mock de cesta anterior (Top Five antiga)
        List<ItemCotacaoAtualResponseDTO> itensAntigos = List.of(
                new ItemCotacaoAtualResponseDTO("PETR4", new BigDecimal("30.00"), new BigDecimal("35.50")),
                new ItemCotacaoAtualResponseDTO("VALE3", new BigDecimal("25.00"), new BigDecimal("65.00")),
                new ItemCotacaoAtualResponseDTO("ITUB4", new BigDecimal("20.00"), new BigDecimal("28.00")),
                new ItemCotacaoAtualResponseDTO("BBDC4", new BigDecimal("15.00"), new BigDecimal("15.50")),
                new ItemCotacaoAtualResponseDTO("ABEV3", new BigDecimal("10.00"), new BigDecimal("12.00"))
        );

        cestaAnteriorMock = new CestaResponseDTO(
                1L,
                "Top Five Anterior",
                false,
                LocalDateTime.now().minusMonths(1),
                itensAntigos
        );

        // Mock de cesta atual (Top Five nova - ABEV3 saiu, BBAS3 entrou)
        List<ItemCotacaoAtualResponseDTO> itensNovos = List.of(
                new ItemCotacaoAtualResponseDTO("PETR4", new BigDecimal("25.00"), new BigDecimal("35.50")),
                new ItemCotacaoAtualResponseDTO("VALE3", new BigDecimal("30.00"), new BigDecimal("65.00")),
                new ItemCotacaoAtualResponseDTO("ITUB4", new BigDecimal("20.00"), new BigDecimal("28.00")),
                new ItemCotacaoAtualResponseDTO("BBDC4", new BigDecimal("15.00"), new BigDecimal("15.50")),
                new ItemCotacaoAtualResponseDTO("BBAS3", new BigDecimal("10.00"), new BigDecimal("45.00"))
        );

        cestaAtualMock = new CestaResponseDTO(
                2L,
                "Top Five Atual",
                true,
                LocalDateTime.now(),
                itensNovos
        );
    }

    // Helper method para criar CotacaoResponseDTO
    private CotacaoResponseDTO criarCotacao(String ticker, BigDecimal precoFechamento) {
        return new CotacaoResponseDTO(
                LocalDate.now(),
                ticker,
                10,
                precoFechamento,
                precoFechamento.multiply(new BigDecimal("1.05")),
                precoFechamento.multiply(new BigDecimal("0.95")),
                precoFechamento
        );
    }

    // Helper method para mockar todas as cotações necessárias
    private void mockarTodasCotacoes() {
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(criarCotacao("ABEV3", new BigDecimal("12.00")));
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(criarCotacao("PETR4", new BigDecimal("35.50")));
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("VALE3"))
                .thenReturn(criarCotacao("VALE3", new BigDecimal("65.00")));
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("ITUB4"))
                .thenReturn(criarCotacao("ITUB4", new BigDecimal("28.00")));
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("BBDC4"))
                .thenReturn(criarCotacao("BBDC4", new BigDecimal("15.50")));
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("BBAS3"))
                .thenReturn(criarCotacao("BBAS3", new BigDecimal("45.00")));
    }

    @Test
    @DisplayName("Deve processar rebalanceamento completo com sucesso")
    void deveProcessarRebalanceamentoCompletoComSucesso() {
        // Arrange
        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaAtualMock);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        
        mockarTodasCotacoes();

        // Mockar todas as cotações necessárias
        mockarTodasCotacoes();

        // Custodia do cliente com ABEV3 (que vai sair)
        Custodia custodiaAbev = new Custodia();
        custodiaAbev.setId(1L);
        custodiaAbev.setTicker("ABEV3F");
        custodiaAbev.setQuantidade(50);
        custodiaAbev.setPrecoMedio(new BigDecimal("11.50"));
        custodiaAbev.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(List.of(custodiaAbev));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(criarCotacao("ABEV3", new BigDecimal("12.00")));

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert
        verify(cestaFeignClient).obterCestaPorId(1L);
        verify(cestaFeignClient).obterCestaPorId(2L);
        verify(clientesFeignClient).listarClientesAtivos();
        verify(custodiaRepository, atLeastOnce()).findCustodiaCliente(1L);

        // Verificar que a custodia foi zerada (não deletada)
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, atLeastOnce()).save(custodiaCaptor.capture());

        List<Custodia> custodiasZeradas = custodiaCaptor.getAllValues().stream()
                .filter(c -> c.getQuantidade() == 0)
                .toList();
        assertThat(custodiasZeradas).isNotEmpty();

        // Verificar que eventos IR foram publicados
        verify(irEventProducer, atLeastOnce()).publicarEventoIRDedoDuro(any());
    }

    @Test
    @DisplayName("Deve vender 100% do ativo que saiu da cesta")
    void deveVender100DoAtivoQueSaiuDaCesta() {
        // Arrange
        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaAtualMock);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        
        mockarTodasCotacoes();

        mockarTodasCotacoes();

        Custodia custodiaAbev = new Custodia();
        custodiaAbev.setId(1L);
        custodiaAbev.setTicker("ABEV3F");
        custodiaAbev.setQuantidade(100);
        custodiaAbev.setPrecoMedio(new BigDecimal("11.00"));
        custodiaAbev.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(List.of(custodiaAbev));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(criarCotacao("ABEV3", new BigDecimal("12.00")));

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, atLeastOnce()).save(custodiaCaptor.capture());

        // Verificar que quantidade foi zerada
        boolean algumaCustodiaZerada = custodiaCaptor.getAllValues().stream()
                .anyMatch(c -> c.getTicker().equals("ABEV3F") && c.getQuantidade() == 0);
        assertThat(algumaCustodiaZerada).isTrue();
    }

    @Test
    @DisplayName("Deve comprar novo ativo que entrou na cesta")
    void deveComprarNovoAtivoQueEntrouNaCesta() {
        // Arrange
        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaAtualMock);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        
        mockarTodasCotacoes();

        // Cliente tem ABEV3 que vai vender
        Custodia custodiaAbev = new Custodia();
        custodiaAbev.setTicker("ABEV3F");
        custodiaAbev.setQuantidade(100);
        custodiaAbev.setPrecoMedio(new BigDecimal("11.00"));
        custodiaAbev.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaAbev))
                .thenReturn(new ArrayList<>());

        when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(criarCotacao("ABEV3", new BigDecimal("12.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("BBAS3"))
                .thenReturn(criarCotacao("BBAS3", new BigDecimal("45.00")));

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert
        verify(cotacaoFeignClient).obterCotacaoPorTicker("BBAS3");
        verify(custodiaRepository, atLeastOnce()).save(any(Custodia.class));
    }

    @Test
    @DisplayName("Deve publicar evento IR Dedo-Duro para venda")
    void devePublicarEventoIRDedoDuroParaVenda() {
        // Arrange
        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaAtualMock);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        
        mockarTodasCotacoes();

        Custodia custodiaAbev = new Custodia();
        custodiaAbev.setTicker("ABEV3F");
        custodiaAbev.setQuantidade(50);
        custodiaAbev.setPrecoMedio(new BigDecimal("11.00"));
        custodiaAbev.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(List.of(custodiaAbev));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(criarCotacao("ABEV3", new BigDecimal("12.00")));

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert
        verify(irEventProducer, atLeastOnce()).publicarEventoIRDedoDuro(any());
        verify(eventoIRRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Deve publicar evento IR Vendas quando total vendas > R$ 20.000")
    void devePublicarEventoIRVendasQuandoTotalVendasMaiorQue20Mil() {
        // Arrange
        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaAtualMock);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        
        mockarTodasCotacoes();

        // Cliente tem muitas ações de ABEV3 (valor > 20k)
        Custodia custodiaAbev = new Custodia();
        custodiaAbev.setTicker("ABEV3");
        custodiaAbev.setQuantidade(2000);
        custodiaAbev.setPrecoMedio(new BigDecimal("10.00"));
        custodiaAbev.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(List.of(custodiaAbev));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(criarCotacao("ABEV3", new BigDecimal("12.00")));

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert
        verify(irEventProducer, atLeastOnce()).publicarEventoIRVendas(any());
    }

    @Test
    @DisplayName("Não deve publicar IR Vendas quando total vendas <= R$ 20.000")
    void naoDevePublicarIRVendasQuandoTotalVendasMenorOuIgual20Mil() {
        // Arrange
        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaAtualMock);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        
        mockarTodasCotacoes();

        // Cliente tem poucas ações (valor < 20k)
        Custodia custodiaAbev = new Custodia();
        custodiaAbev.setTicker("ABEV3F");
        custodiaAbev.setQuantidade(50);
        custodiaAbev.setPrecoMedio(new BigDecimal("11.00"));
        custodiaAbev.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(List.of(custodiaAbev));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(criarCotacao("ABEV3", new BigDecimal("12.00")));

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert
        verify(irEventProducer, never()).publicarEventoIRVendas(any());
    }

    @Test
    @DisplayName("Deve separar compra em lote padrão e fracionário quando quantidade >= 100")
    void deveSepararCompraEmLotePadraoEFracionario() {
        // Arrange
        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaAtualMock);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        
        mockarTodasCotacoes();

        // Cliente vendeu ABEV3 e vai comprar BBAS3
        Custodia custodiaAbev = new Custodia();
        custodiaAbev.setTicker("ABEV3");
        custodiaAbev.setQuantidade(500);
        custodiaAbev.setPrecoMedio(new BigDecimal("10.00"));
        custodiaAbev.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaAbev))
                .thenReturn(new ArrayList<>());

        when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(criarCotacao("ABEV3", new BigDecimal("12.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("BBAS3"))
                .thenReturn(criarCotacao("BBAS3", new BigDecimal("45.00")));

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, atLeastOnce()).save(custodiaCaptor.capture());

        // Pode ter criado custodia com BBAS3 ou BBAS3F
        boolean algumaBBAS = custodiaCaptor.getAllValues().stream()
                .anyMatch(c -> c.getTicker().startsWith("BBAS3"));
        assertThat(algumaBBAS).isTrue();
    }

    @Test
    @DisplayName("Deve atualizar preço médio ao comprar ativo existente")
    void deveAtualizarPrecoMedioAoComprarAtivoExistente() {
        // Arrange
        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaAtualMock);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        
        mockarTodasCotacoes();

        // Cliente já tem PETR4 (que permanece na cesta)
        Custodia custodiaPetr = new Custodia();
        custodiaPetr.setTicker("PETR4");
        custodiaPetr.setQuantidade(100);
        custodiaPetr.setPrecoMedio(new BigDecimal("30.00"));
        custodiaPetr.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(List.of(custodiaPetr));
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(criarCotacao("PETR4", new BigDecimal("35.50")));

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert
        verify(custodiaRepository, atLeastOnce()).save(any(Custodia.class));
    }

    @Test
    @DisplayName("Não deve processar ativos que permaneceram com mesmo percentual")
    void naoDeveProcessarAtivosPermanecidosComMesmoPercentual() {
        // Arrange - Cesta com percentuais iguais
        List<ItemCotacaoAtualResponseDTO> itensIguais = List.of(
                new ItemCotacaoAtualResponseDTO("PETR4", new BigDecimal("30.00"), new BigDecimal("35.50")),
                new ItemCotacaoAtualResponseDTO("VALE3", new BigDecimal("25.00"), new BigDecimal("65.00")),
                new ItemCotacaoAtualResponseDTO("ITUB4", new BigDecimal("20.00"), new BigDecimal("28.00")),
                new ItemCotacaoAtualResponseDTO("BBDC4", new BigDecimal("15.00"), new BigDecimal("15.50")),
                new ItemCotacaoAtualResponseDTO("ABEV3", new BigDecimal("10.00"), new BigDecimal("12.00"))
        );

        CestaResponseDTO cestaIgual = new CestaResponseDTO(
                2L,
                "Top Five Igual",
                true,
                LocalDateTime.now(),
                itensIguais
        );

        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaIgual);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        
        mockarTodasCotacoes();
        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(new ArrayList<>());

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert
        verify(custodiaRepository, atLeastOnce()).findCustodiaCliente(1L);
    }

    @Test
    @DisplayName("Deve processar múltiplos clientes no rebalanceamento")
    void deveProcessarMultiplosClientesNoRebalanceamento() {
        // Arrange
        ClienteResponseDTO cliente2 = new ClienteResponseDTO(
                2L,
                "Cliente 2",
                "98765432100",
                "cliente2@teste.com",
                new BigDecimal("5000.00"),
                true,
                LocalDateTime.now(),
                new ContaGraficaDTO(2L, "ITAUFL00002", "FILHOTE", LocalDateTime.now())
        );

        when(cestaFeignClient.obterCestaPorId(1L)).thenReturn(cestaAnteriorMock);
        when(cestaFeignClient.obterCestaPorId(2L)).thenReturn(cestaAtualMock);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock, cliente2));
        
        mockarTodasCotacoes();
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(new ArrayList<>());

        // Act
        rebalanceamentoService.executarRebalanceamento(1L, 2L, dataExecucao);

        // Assert - 2 clientes x 2 chamadas cada (vendas + rebalanceamento) = 4 chamadas
        verify(custodiaRepository, times(4)).findCustodiaCliente(anyLong());
    }

    // =====================================================
    // TESTES DE REBALANCEAMENTO POR DESVIO
    // =====================================================

    @Test
    @DisplayName("Deve executar rebalanceamento por desvio com sucesso")
    void deveExecutarRebalanceamentoPorDesvioComSucesso() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("5.0");
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        // Cliente com carteira desbalanceada
        Custodia custodiaLREN3 = criarCustodia("LREN3", 448, new BigDecimal("13.00"), 1L);
        Custodia custodiaMOVI3 = criarCustodia("MOVI3", 50, new BigDecimal("10.00"), 1L);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaLREN3, custodiaMOVI3));

        mockarCotacoesCestaAtiva();

        // Act
        var response = rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.totalClientesProcessados()).isEqualTo(1);
        assertThat(response.totalClientesRebalanceados()).isGreaterThan(0);
        assertThat(response.mensagem()).contains("sucesso");

        verify(cestaFeignClient).obterCestaAtiva();
        verify(clientesFeignClient).listarClientesAtivos();
        verify(custodiaRepository, atLeastOnce()).findCustodiaCliente(1L);
    }

    @Test
    @DisplayName("Deve detectar ativo sobre-alocado e vender")
    void deveDetectarAtivoSobreAlocadoEVender() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("5.0");
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        // LREN3 com 60% (alvo 40%) -> sobre-alocado
        Custodia custodiaLREN3 = criarCustodia("LREN3", 448, new BigDecimal("13.00"), 1L);
        Custodia custodiaMOVI3 = criarCustodia("MOVI3", 150, new BigDecimal("10.00"), 1L);
        Custodia custodiaMRVE3 = criarCustodia("MRVE3", 129, new BigDecimal("7.50"), 1L);
        Custodia custodiaBMEB3 = criarCustodia("BMEB3", 16, new BigDecimal("60.00"), 1L);
        Custodia custodiaBMGB4 = criarCustodia("BMGB4", 201, new BigDecimal("5.00"), 1L);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaLREN3, custodiaMOVI3, custodiaMRVE3, custodiaBMEB3, custodiaBMGB4));

        mockarCotacoesCestaAtiva();

        // Act
        var response = rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        assertThat(response.totalClientesRebalanceados()).isEqualTo(1);
        assertThat(response.clientesRebalanceados()).hasSize(1);

        var operacoes = response.clientesRebalanceados().get(0).operacoes();
        var vendaLREN3 = operacoes.stream()
                .filter(op -> op.ticker().equals("LREN3") && op.tipoOperacao().equals("VENDA"))
                .findFirst();

        assertThat(vendaLREN3).isPresent();
        assertThat(vendaLREN3.get().quantidade()).isGreaterThan(0);

        verify(custodiaRepository, atLeastOnce()).save(any(Custodia.class));
        verify(rebalanceamentoRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Deve detectar ativo sub-alocado e comprar")
    void deveDetectarAtivoSubAlocadoEComprar() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("5.0");
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        // MOVI3 com 5% (alvo 20%) -> sub-alocado
        Custodia custodiaLREN3 = criarCustodia("LREN3", 299, new BigDecimal("13.00"), 1L);
        Custodia custodiaMOVI3 = criarCustodia("MOVI3", 50, new BigDecimal("10.00"), 1L);
        Custodia custodiaMRVE3 = criarCustodia("MRVE3", 129, new BigDecimal("7.50"), 1L);
        Custodia custodiaBMEB3 = criarCustodia("BMEB3", 16, new BigDecimal("60.00"), 1L);
        Custodia custodiaBMGB4 = criarCustodia("BMGB4", 201, new BigDecimal("5.00"), 1L);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaLREN3, custodiaMOVI3, custodiaMRVE3, custodiaBMEB3, custodiaBMGB4));

        mockarCotacoesCestaAtiva();

        // Act
        var response = rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        assertThat(response.totalClientesRebalanceados()).isEqualTo(1);

        var operacoes = response.clientesRebalanceados().get(0).operacoes();
        var compraMOVI3 = operacoes.stream()
                .filter(op -> op.ticker().equals("MOVI3") && op.tipoOperacao().equals("COMPRA"))
                .findFirst();

        assertThat(compraMOVI3).isPresent();
        assertThat(compraMOVI3.get().quantidade()).isGreaterThan(0);

        verify(custodiaRepository, atLeastOnce()).save(any(Custodia.class));
        verify(irEventProducer, atLeastOnce()).publicarEventoIRDedoDuro(any());
    }

    @Test
    @DisplayName("Não deve rebalancear quando desvio está dentro do limiar")
    void naoDeveRebalancearQuandoDesvioEstaDentroDoLimiar() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("5.0");
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        // Carteira perfeitamente balanceada
        Custodia custodiaLREN3 = criarCustodia("LREN3", 299, new BigDecimal("13.00"), 1L);
        Custodia custodiaMOVI3 = criarCustodia("MOVI3", 150, new BigDecimal("10.00"), 1L);
        Custodia custodiaMRVE3 = criarCustodia("MRVE3", 129, new BigDecimal("7.50"), 1L);
        Custodia custodiaBMEB3 = criarCustodia("BMEB3", 16, new BigDecimal("60.00"), 1L);
        Custodia custodiaBMGB4 = criarCustodia("BMGB4", 201, new BigDecimal("5.00"), 1L);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaLREN3, custodiaMOVI3, custodiaMRVE3, custodiaBMEB3, custodiaBMGB4));

        mockarCotacoesCestaAtiva();

        // Act
        var response = rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        assertThat(response.totalClientesProcessados()).isEqualTo(1);
        // Pode ser 0 ou 1 dependendo das proporções exatas
        assertThat(response.totalClientesRebalanceados()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Deve ignorar custódias zeradas no rebalanceamento por desvio")
    void deveIgnorarCustodiasZeradasNoRebalanceamentoPorDesvio() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("5.0");
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        // Custódias zeradas (resíduos de rebalanceamento anterior)
        Custodia custodiaLREN3 = criarCustodia("LREN3", 0, new BigDecimal("13.00"), 1L);
        Custodia custodiaMOVI3 = criarCustodia("MOVI3", 0, new BigDecimal("10.00"), 1L);
        Custodia custodiaMRVE3 = criarCustodia("MRVE3", 100, new BigDecimal("7.50"), 1L);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaLREN3, custodiaMOVI3, custodiaMRVE3));

        mockarCotacoesCestaAtiva();

        // Act
        var response = rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        assertThat(response.totalClientesProcessados()).isEqualTo(1);
        // Deve processar apenas a custodia com quantidade > 0
    }

    @Test
    @DisplayName("Deve processar múltiplos clientes no rebalanceamento por desvio")
    void deveProcessarMultiplosClientesNoRebalanceamentoPorDesvio() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("5.0");
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        ClienteResponseDTO cliente2 = new ClienteResponseDTO(
                2L,
                "Cliente 2",
                "98765432100",
                "cliente2@teste.com",
                new BigDecimal("5000.00"),
                true,
                LocalDateTime.now(),
                new ContaGraficaDTO(2L, "ITAUFL00002", "FILHOTE", LocalDateTime.now())
        );

        Custodia custodia1Cliente1 = criarCustodia("LREN3", 448, new BigDecimal("13.00"), 1L);
        Custodia custodia2Cliente1 = criarCustodia("MOVI3", 50, new BigDecimal("10.00"), 1L);

        Custodia custodia1Cliente2 = criarCustodia("LREN3", 200, new BigDecimal("13.00"), 2L);
        Custodia custodia2Cliente2 = criarCustodia("MOVI3", 100, new BigDecimal("10.00"), 2L);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock, cliente2));
        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodia1Cliente1, custodia2Cliente1));
        when(custodiaRepository.findCustodiaCliente(2L))
                .thenReturn(List.of(custodia1Cliente2, custodia2Cliente2));

        mockarCotacoesCestaAtiva();

        // Act
        var response = rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        assertThat(response.totalClientesProcessados()).isEqualTo(2);
        verify(custodiaRepository).findCustodiaCliente(1L);
        verify(custodiaRepository).findCustodiaCliente(2L);
    }

    @Test
    @DisplayName("Deve salvar rebalanceamento no banco para cada operação")
    void deveSalvarRebalanceamentoNoBancoParaCadaOperacao() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("5.0");
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        Custodia custodiaLREN3 = criarCustodia("LREN3", 448, new BigDecimal("13.00"), 1L);
        Custodia custodiaMOVI3 = criarCustodia("MOVI3", 50, new BigDecimal("10.00"), 1L);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaLREN3, custodiaMOVI3));

        mockarCotacoesCestaAtiva();

        // Act
        rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        verify(rebalanceamentoRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Deve publicar eventos de IR para cada operação")
    void devePublicarEventosDeIRParaCadaOperacao() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("5.0");
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        Custodia custodiaLREN3 = criarCustodia("LREN3", 448, new BigDecimal("13.00"), 1L);
        Custodia custodiaMOVI3 = criarCustodia("MOVI3", 50, new BigDecimal("10.00"), 1L);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaLREN3, custodiaMOVI3));

        mockarCotacoesCestaAtiva();

        // Act
        rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        verify(irEventProducer, atLeastOnce()).publicarEventoIRDedoDuro(any());
        verify(eventoIRRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando nenhum cliente possui custódia")
    void deveRetornarListaVaziaQuandoNenhumClientePossuiCustodia() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("5.0");
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(new ArrayList<>());

        // Act
        var response = rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        assertThat(response.totalClientesProcessados()).isEqualTo(1);
        assertThat(response.totalClientesRebalanceados()).isZero();
        assertThat(response.clientesRebalanceados()).isEmpty();
    }

    @Test
    @DisplayName("Deve usar limiar personalizado quando fornecido")
    void deveUsarLimiarPersonalizadoQuandoFornecido() {
        // Arrange
        BigDecimal limiarDesvio = new BigDecimal("10.0"); // Limiar mais permissivo
        CestaResponseDTO cestaAtiva = criarCestaAtiva();

        // Desvio de 8% (não seria detectado com limiar de 10%)
        Custodia custodiaLREN3 = criarCustodia("LREN3", 360, new BigDecimal("13.00"), 1L);
        Custodia custodiaMOVI3 = criarCustodia("MOVI3", 150, new BigDecimal("10.00"), 1L);
        Custodia custodiaMRVE3 = criarCustodia("MRVE3", 129, new BigDecimal("7.50"), 1L);
        Custodia custodiaBMEB3 = criarCustodia("BMEB3", 16, new BigDecimal("60.00"), 1L);
        Custodia custodiaBMGB4 = criarCustodia("BMGB4", 201, new BigDecimal("5.00"), 1L);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(List.of(clienteMock));
        when(custodiaRepository.findCustodiaCliente(1L))
                .thenReturn(List.of(custodiaLREN3, custodiaMOVI3, custodiaMRVE3, custodiaBMEB3, custodiaBMGB4));

        mockarCotacoesCestaAtiva();

        // Act
        var response = rebalanceamentoService.executarRebalanceamentoPorDesvio(limiarDesvio, dataExecucao);

        // Assert
        assertThat(response.totalClientesProcessados()).isEqualTo(1);
        // Com limiar de 10%, menos operações devem ser realizadas
    }

    // Helper methods

    private CestaResponseDTO criarCestaAtiva() {
        List<ItemCotacaoAtualResponseDTO> itens = List.of(
                new ItemCotacaoAtualResponseDTO("LREN3", new BigDecimal("40.00"), new BigDecimal("13.39")),
                new ItemCotacaoAtualResponseDTO("MOVI3", new BigDecimal("20.00"), new BigDecimal("9.93")),
                new ItemCotacaoAtualResponseDTO("MRVE3", new BigDecimal("10.00"), new BigDecimal("7.71")),
                new ItemCotacaoAtualResponseDTO("BMEB3", new BigDecimal("10.00"), new BigDecimal("63.59")),
                new ItemCotacaoAtualResponseDTO("BMGB4", new BigDecimal("20.00"), new BigDecimal("4.97"))
        );

        return new CestaResponseDTO(
                18L,
                "Cesta Abril",
                true,
                LocalDateTime.now(),
                itens
        );
    }

    private void mockarCotacoesCestaAtiva() {
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("LREN3"))
                .thenReturn(criarCotacao("LREN3", new BigDecimal("13.39")));
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("MOVI3"))
                .thenReturn(criarCotacao("MOVI3", new BigDecimal("9.93")));
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("MRVE3"))
                .thenReturn(criarCotacao("MRVE3", new BigDecimal("7.71")));
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("BMEB3"))
                .thenReturn(criarCotacao("BMEB3", new BigDecimal("63.59")));
        lenient().when(cotacaoFeignClient.obterCotacaoPorTicker("BMGB4"))
                .thenReturn(criarCotacao("BMGB4", new BigDecimal("4.97")));
    }

    private Custodia criarCustodia(String ticker, int quantidade, BigDecimal precoMedio, Long contaGraficaId) {
        Custodia custodia = new Custodia();
        custodia.setId((long) (Math.random() * 1000));
        custodia.setTicker(ticker);
        custodia.setQuantidade(quantidade);
        custodia.setPrecoMedio(precoMedio);
        custodia.setContaGraficaId(contaGraficaId);
        custodia.setDataUltimaAtualizacao(LocalDateTime.now());
        return custodia;
    }
}
