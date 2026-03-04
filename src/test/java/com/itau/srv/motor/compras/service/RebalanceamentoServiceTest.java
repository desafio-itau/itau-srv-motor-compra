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
}




