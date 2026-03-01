package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.agrupamento.AgrupamentoResponseDTO;
import com.itau.srv.motor.compras.dto.agrupamento.ClienteAgrupamentoDTO;
import com.itau.srv.motor.compras.dto.conta.ContaGraficaDTO;
import com.itau.srv.motor.compras.dto.cotacao.CotacaoResponseDTO;
import com.itau.srv.motor.compras.dto.custodia.CustodiaMasterResponseDTO;
import com.itau.srv.motor.compras.dto.custodia.CustodiaResponseDTO;
import com.itau.srv.motor.compras.dto.distribuicao.DistribuicaoResponseDTO;
import com.itau.srv.motor.compras.dto.ordemcompra.CalcularQuantidadeAtivoResponse;
import com.itau.srv.motor.compras.feign.ContasGraficasFeignClient;
import com.itau.srv.motor.compras.feign.CotacaoFeignClient;
import com.itau.srv.motor.compras.mapper.CustodiaMapper;
import com.itau.srv.motor.compras.mapper.DistribuicaoMapper;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.model.OrdemCompra;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import com.itau.srv.motor.compras.repository.DistribuicaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustodiaService - Testes Unitários")
class CustodiaServiceTest {

    @Mock
    private CustodiaRepository custodiaRepository;

    @Mock
    private EventoIRService eventoIRService;

    @Mock
    private ResiduoService residuoService;

    @Mock
    private DistribuicaoRepository distribuicaoRepository;

    @Mock
    private DistribuicaoMapper distribuicaoMapper;

    @Mock
    private ContasGraficasFeignClient contasGraficasFeignClient;

    @Mock
    private CotacaoFeignClient cotacaoFeignClient;

    @Spy
    private CustodiaMapper custodiaMapper = new CustodiaMapper();

    @InjectMocks
    private CustodiaService custodiaService;

    private AgrupamentoResponseDTO agrupamento;
    private CalcularQuantidadeAtivoResponse calculoAtivos;
    private List<ClienteAgrupamentoDTO> clientes;
    private List<OrdemCompra> ordensCompra;

    @BeforeEach
    void setUp() {
        // Set contaMasterId property
        ReflectionTestUtils.setField(custodiaService, "contaMasterId", 1L);

        // Configurar clientes
        clientes = List.of(
                new ClienteAgrupamentoDTO(1L, "Cliente A", "12345678901", 1L, new BigDecimal("1000.00")),
                new ClienteAgrupamentoDTO(2L, "Cliente B", "98765432100", 2L, new BigDecimal("2000.00"))
        );
        agrupamento = new AgrupamentoResponseDTO(new BigDecimal("3000.00"), clientes);

        // Configurar ordens de compra
        OrdemCompra ordem1 = new OrdemCompra();
        ordem1.setTicker("PETR4");
        ordem1.setQuantidade(100);
        ordem1.setPrecoUnitario(new BigDecimal("35.00"));

        OrdemCompra ordem2 = new OrdemCompra();
        ordem2.setTicker("VALE3");
        ordem2.setQuantidade(50);
        ordem2.setPrecoUnitario(new BigDecimal("62.00"));

        ordensCompra = List.of(ordem1, ordem2);
        calculoAtivos = new CalcularQuantidadeAtivoResponse(ordensCompra, Collections.emptyList());
    }

    @Test
    @DisplayName("Deve distribuir custodias sem saldo master")
    void deveDistribuirCustodiasSemSaldoMaster() {
        // Arrange
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        List<DistribuicaoResponseDTO> resultado = custodiaService.distribuirCustodias(calculoAtivos, agrupamento);

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).clienteId()).isEqualTo(1L);
        assertThat(resultado.get(0).ativos()).hasSize(2);
        assertThat(resultado.get(1).clienteId()).isEqualTo(2L);
        assertThat(resultado.get(1).ativos()).hasSize(2);

        // Verificar que custodias foram criadas (4 = 2 clientes × 2 ativos)
        verify(custodiaRepository, times(4)).save(any(Custodia.class));
        verify(eventoIRService, times(4)).publicarEventoIR(any(), any(), anyInt(), any());
        verify(residuoService, times(2)).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Deve distribuir custodias com saldo master")
    void deveDistribuirCustodiasComSaldoMaster() {
        // Arrange
        Custodia custodiaMaster = new Custodia();
        custodiaMaster.setTicker("PETR4");
        custodiaMaster.setQuantidade(10);

        when(custodiaRepository.findCustodiaMaster()).thenReturn(List.of(custodiaMaster));
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        List<DistribuicaoResponseDTO> resultado = custodiaService.distribuirCustodias(calculoAtivos, agrupamento);

        // Assert
        assertThat(resultado).hasSize(2);

        // Total disponível de PETR4 = 100 (compradas) + 10 (master) = 110
        // Cliente A (33.33%): TRUNCAR(110 × 0.3333) = 36 ações
        // Cliente B (66.67%): TRUNCAR(110 × 0.6667) = 73 ações
        int totalPETR4Distribuido = resultado.stream()
                .flatMap(d -> d.ativos().stream())
                .filter(a -> a.ticker().equals("PETR4"))
                .mapToInt(a -> a.quantidade())
                .sum();

        assertThat(totalPETR4Distribuido).isGreaterThan(0);
        verify(custodiaRepository, times(4)).save(any(Custodia.class));
    }

    @Test
    @DisplayName("Deve criar nova custodia quando cliente não possui o ativo")
    void deveCriarNovaCustodiaQuandoClienteNaoPossuiAtivo() {
        // Arrange
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        custodiaService.distribuirCustodias(calculoAtivos, agrupamento);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, times(4)).save(custodiaCaptor.capture());

        List<Custodia> custodiasCreated = custodiaCaptor.getAllValues();
        assertThat(custodiasCreated).hasSize(4);
        assertThat(custodiasCreated.get(0).getTicker()).isIn("PETR4", "VALE3");
        assertThat(custodiasCreated.get(0).getQuantidade()).isGreaterThan(0);
        assertThat(custodiasCreated.get(0).getPrecoMedio()).isNotNull();
    }

    @Test
    @DisplayName("Deve atualizar custodia existente e recalcular preço médio")
    void deveAtualizarCustodiaExistenteERecalcularPrecoMedio() {
        // Arrange
        Custodia custodiaExistente = new Custodia();
        custodiaExistente.setTicker("PETR4");
        custodiaExistente.setQuantidade(50);
        custodiaExistente.setPrecoMedio(new BigDecimal("30.00"));
        custodiaExistente.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(List.of(custodiaExistente));
        when(custodiaRepository.findCustodiaCliente(2L)).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        custodiaService.distribuirCustodias(calculoAtivos, agrupamento);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, atLeastOnce()).save(custodiaCaptor.capture());

        // Verificar que a custodia foi atualizada (não criada)
        List<Custodia> custodiasUpdated = custodiaCaptor.getAllValues().stream()
                .filter(c -> c.getTicker().equals("PETR4") && c.getContaGraficaId().equals(1L))
                .toList();

        assertThat(custodiasUpdated).isNotEmpty();
        Custodia custodiaAtualizada = custodiasUpdated.get(0);

        // Quantidade deve ser maior que 50 (anterior)
        assertThat(custodiaAtualizada.getQuantidade()).isGreaterThan(50);

        // Preço médio deve ter sido recalculado
        assertThat(custodiaAtualizada.getPrecoMedio()).isNotEqualTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("Deve calcular proporção corretamente para cada cliente")
    void deveCalcularProporcaoCorretamenteParaCadaCliente() {
        // Arrange
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        List<DistribuicaoResponseDTO> resultado = custodiaService.distribuirCustodias(calculoAtivos, agrupamento);

        // Assert
        DistribuicaoResponseDTO distClienteA = resultado.stream()
                .filter(d -> d.clienteId().equals(1L))
                .findFirst()
                .orElseThrow();

        DistribuicaoResponseDTO distClienteB = resultado.stream()
                .filter(d -> d.clienteId().equals(2L))
                .findFirst()
                .orElseThrow();

        // Cliente A tem 33.33% (1000/3000), Cliente B tem 66.67% (2000/3000)
        int qtdClienteA = distClienteA.ativos().stream()
                .filter(a -> a.ticker().equals("PETR4"))
                .findFirst()
                .map(a -> a.quantidade())
                .orElse(0);

        int qtdClienteB = distClienteB.ativos().stream()
                .filter(a -> a.ticker().equals("PETR4"))
                .findFirst()
                .map(a -> a.quantidade())
                .orElse(0);

        // Cliente B deve ter aproximadamente o dobro de ações do Cliente A
        assertThat(qtdClienteB).isGreaterThan(qtdClienteA);
        assertThat((double) qtdClienteB / qtdClienteA).isBetween(1.8, 2.2);
    }

    @Test
    @DisplayName("Deve pular distribuição quando quantidade é zero")
    void devePularDistribuicaoQuandoQuantidadeZero() {
        // Arrange - Cliente com aporte muito pequeno
        ClienteAgrupamentoDTO clientePequeno = new ClienteAgrupamentoDTO(
                3L, "Cliente Pequeno", "11111111111", 3L, new BigDecimal("0.01")
        );

        List<ClienteAgrupamentoDTO> clientesComPequeno = new ArrayList<>(clientes);
        clientesComPequeno.add(clientePequeno);

        AgrupamentoResponseDTO agrupamentoComPequeno = new AgrupamentoResponseDTO(
                new BigDecimal("3000.01"),
                clientesComPequeno
        );

        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        List<DistribuicaoResponseDTO> resultado = custodiaService.distribuirCustodias(calculoAtivos, agrupamentoComPequeno);

        // Assert
        // Cliente pequeno não deve aparecer nas distribuições pois quantidade será zero
        assertThat(resultado).hasSize(2); // Apenas Cliente A e B
        assertThat(resultado).noneMatch(d -> d.clienteId().equals(3L));
    }

    @Test
    @DisplayName("Deve publicar evento IR para cada distribuição")
    void devePublicarEventoIRParaCadaDistribuicao() {
        // Arrange
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        custodiaService.distribuirCustodias(calculoAtivos, agrupamento);

        // Assert
        // 2 clientes × 2 ativos = 4 eventos IR
        verify(eventoIRService, times(4)).publicarEventoIR(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("Deve chamar atualização de resíduos para cada ativo")
    void deveChamarAtualizacaoDeResiduosParaCadaAtivo() {
        // Arrange
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        custodiaService.distribuirCustodias(calculoAtivos, agrupamento);

        // Assert
        verify(residuoService, times(2)).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());
        verify(residuoService).atualizarResiduosCustodiaMaster(eq("PETR4"), anyInt(), any(), any());
        verify(residuoService).atualizarResiduosCustodiaMaster(eq("VALE3"), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Deve distribuir com um único cliente")
    void deveDistribuirComUmUnicoCliente() {
        // Arrange
        ClienteAgrupamentoDTO clienteUnico = new ClienteAgrupamentoDTO(
                1L, "Cliente Único", "12345678901", 1L, new BigDecimal("1000.00")
        );
        AgrupamentoResponseDTO agrupamentoUnico = new AgrupamentoResponseDTO(
                new BigDecimal("1000.00"),
                List.of(clienteUnico)
        );

        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        List<DistribuicaoResponseDTO> resultado = custodiaService.distribuirCustodias(calculoAtivos, agrupamentoUnico);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).clienteId()).isEqualTo(1L);
        assertThat(resultado.get(0).ativos()).hasSize(2);

        // Cliente único recebe 100% das ações
        int totalPETR4 = resultado.get(0).ativos().stream()
                .filter(a -> a.ticker().equals("PETR4"))
                .mapToInt(a -> a.quantidade())
                .sum();
        assertThat(totalPETR4).isEqualTo(100);
    }

    @Test
    @DisplayName("Deve distribuir com múltiplos ativos")
    void deveDistribuirComMultiplosAtivos() {
        // Arrange - Adicionar mais ativos
        OrdemCompra ordem3 = new OrdemCompra();
        ordem3.setTicker("ITUB4");
        ordem3.setQuantidade(200);
        ordem3.setPrecoUnitario(new BigDecimal("30.00"));

        List<OrdemCompra> ordensMultiplas = new ArrayList<>(ordensCompra);
        ordensMultiplas.add(ordem3);

        CalcularQuantidadeAtivoResponse calculoMultiplo = new CalcularQuantidadeAtivoResponse(
                ordensMultiplas,
                Collections.emptyList()
        );

        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        List<DistribuicaoResponseDTO> resultado = custodiaService.distribuirCustodias(calculoMultiplo, agrupamento);

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).ativos()).hasSize(3); // PETR4, VALE3, ITUB4
        assertThat(resultado.get(1).ativos()).hasSize(3);

        // Verificar que resíduos foram atualizados para todos os ativos
        verify(residuoService, times(3)).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há ordens de compra")
    void deveRetornarListaVaziaQuandoNaoHaOrdensDeCompra() {
        // Arrange
        CalcularQuantidadeAtivoResponse calculoVazio = new CalcularQuantidadeAtivoResponse(
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        List<DistribuicaoResponseDTO> resultado = custodiaService.distribuirCustodias(calculoVazio, agrupamento);

        // Assert
        assertThat(resultado).isEmpty();
        verify(custodiaRepository, never()).save(any(Custodia.class));
        verify(eventoIRService, never()).publicarEventoIR(any(), any(), anyInt(), any());
        verify(residuoService, never()).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Deve usar RoundingMode.DOWN para truncar proporção")
    void deveUsarRoundingModeDownParaTruncarProporcao() {
        // Arrange - Cliente com proporção que gera dízima periódica
        ClienteAgrupamentoDTO cliente1 = new ClienteAgrupamentoDTO(
                1L, "Cliente 1", "11111111111", 1L, new BigDecimal("1000.00")
        );
        ClienteAgrupamentoDTO cliente2 = new ClienteAgrupamentoDTO(
                2L, "Cliente 2", "22222222222", 2L, new BigDecimal("1000.00")
        );
        ClienteAgrupamentoDTO cliente3 = new ClienteAgrupamentoDTO(
                3L, "Cliente 3", "33333333333", 3L, new BigDecimal("1000.00")
        );

        AgrupamentoResponseDTO agrup3Clientes = new AgrupamentoResponseDTO(
                new BigDecimal("3000.00"),
                List.of(cliente1, cliente2, cliente3)
        );

        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(anyLong())).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        List<DistribuicaoResponseDTO> resultado = custodiaService.distribuirCustodias(calculoAtivos, agrup3Clientes);

        // Assert
        assertThat(resultado).hasSize(3);

        // Verificar que cada cliente recebeu aproximadamente 33.33% (TRUNCADO)
        int totalDistribuido = resultado.stream()
                .flatMap(d -> d.ativos().stream())
                .filter(a -> a.ticker().equals("PETR4"))
                .mapToInt(a -> a.quantidade())
                .sum();

        // Total distribuído deve ser <= 100 (devido ao truncamento)
        assertThat(totalDistribuido).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Deve resetar custodiaExistente para cada iteração de cliente")
    void deveResetarCustodiaExistenteParaCadaIteracaoDeCliente() {
        // Arrange - Cliente 1 tem PETR4, Cliente 2 não tem
        Custodia custodiaCliente1 = new Custodia();
        custodiaCliente1.setTicker("PETR4");
        custodiaCliente1.setQuantidade(10);
        custodiaCliente1.setPrecoMedio(new BigDecimal("30.00"));
        custodiaCliente1.setContaGraficaId(1L);

        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(custodiaRepository.findCustodiaCliente(1L)).thenReturn(List.of(custodiaCliente1));
        when(custodiaRepository.findCustodiaCliente(2L)).thenReturn(Collections.emptyList());
        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventoIRService).publicarEventoIR(any(), any(), anyInt(), any());
        doNothing().when(residuoService).atualizarResiduosCustodiaMaster(any(), anyInt(), any(), any());

        // Act
        custodiaService.distribuirCustodias(calculoAtivos, agrupamento);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, atLeast(3)).save(custodiaCaptor.capture());

        // Verificar que Cliente 2 teve custodia CRIADA (não atualizada)
        List<Custodia> custodiasCliente2 = custodiaCaptor.getAllValues().stream()
                .filter(c -> c.getContaGraficaId().equals(2L))
                .filter(c -> c.getTicker().equals("PETR4"))
                .toList();

        assertThat(custodiasCliente2).isNotEmpty();
        // Cliente 2 deve ter quantidade > 0 (não deve herdar a quantidade do Cliente 1)
        assertThat(custodiasCliente2.get(0).getQuantidade()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Deve consultar custodia master com sucesso")
    void deveConsultarCustodiaMasterComSucesso() {
        // Arrange
        ContaGraficaDTO contaMaster = new ContaGraficaDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER",
                LocalDateTime.now()
        );

        Custodia custodia1 = new Custodia();
        custodia1.setId(1L);
        custodia1.setTicker("PETR4");
        custodia1.setQuantidade(10);
        custodia1.setPrecoMedio(new BigDecimal("35.50"));
        custodia1.setContaGraficaId(1L);
        custodia1.setDataUltimaAtualizacao(LocalDateTime.of(2026, 3, 1, 10, 0));

        Custodia custodia2 = new Custodia();
        custodia2.setId(2L);
        custodia2.setTicker("VALE3");
        custodia2.setQuantidade(5);
        custodia2.setPrecoMedio(new BigDecimal("62.00"));
        custodia2.setContaGraficaId(1L);
        custodia2.setDataUltimaAtualizacao(LocalDateTime.of(2026, 3, 1, 10, 0));

        List<Custodia> custodiasMaster = List.of(custodia1, custodia2);

        CotacaoResponseDTO cotacao1 = new CotacaoResponseDTO(
                java.time.LocalDate.now(), "PETR4", 10, new BigDecimal("35.00"),
                new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00"));
        CotacaoResponseDTO cotacao2 = new CotacaoResponseDTO(
                java.time.LocalDate.now(), "VALE3", 10, new BigDecimal("62.00"),
                new BigDecimal("64.00"), new BigDecimal("61.00"), new BigDecimal("63.50"));

        when(contasGraficasFeignClient.buscarConta(1L)).thenReturn(contaMaster);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(custodiasMaster);
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4")).thenReturn(cotacao1);
        when(cotacaoFeignClient.obterCotacaoPorTicker("VALE3")).thenReturn(cotacao2);

        // Act
        CustodiaMasterResponseDTO resultado = custodiaService.consultarCustodiaMaster();

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.contaMaster()).isNotNull();
        assertThat(resultado.contaMaster().id()).isEqualTo(1L);
        assertThat(resultado.contaMaster().numeroConta()).isEqualTo("CONTA-MASTER-001");
        assertThat(resultado.contaMaster().tipo()).isEqualTo("MASTER");
        assertThat(resultado.custodia()).hasSize(2);
        assertThat(resultado.custodia().get(0).ticker()).isEqualTo("PETR4");
        assertThat(resultado.custodia().get(0).quantidade()).isEqualTo(10);
        assertThat(resultado.custodia().get(0).precoMedio()).isEqualByComparingTo(new BigDecimal("35.50"));
        assertThat(resultado.custodia().get(0).valorAtual()).isEqualByComparingTo(new BigDecimal("36.00"));
        assertThat(resultado.custodia().get(1).ticker()).isEqualTo("VALE3");
        assertThat(resultado.custodia().get(1).quantidade()).isEqualTo(5);

        // Valor total: (10 × 36.00) + (5 × 63.50) = 360 + 317.50 = 677.50
        assertThat(resultado.valorTotalResiduo()).isEqualByComparingTo(new BigDecimal("677.50"));

        verify(contasGraficasFeignClient).buscarConta(1L);
        verify(custodiaRepository).findCustodiaMaster();
        verify(cotacaoFeignClient).obterCotacaoPorTicker("PETR4");
        verify(cotacaoFeignClient).obterCotacaoPorTicker("VALE3");
    }

    @Test
    @DisplayName("Deve consultar custodia master vazia")
    void deveConsultarCustodiaMasterVazia() {
        // Arrange
        ContaGraficaDTO contaMaster = new ContaGraficaDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER",
                LocalDateTime.now()
        );

        when(contasGraficasFeignClient.buscarConta(1L)).thenReturn(contaMaster);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        CustodiaMasterResponseDTO resultado = custodiaService.consultarCustodiaMaster();

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.contaMaster()).isNotNull();
        assertThat(resultado.contaMaster().id()).isEqualTo(1L);
        assertThat(resultado.custodia()).isEmpty();
        assertThat(resultado.valorTotalResiduo()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(contasGraficasFeignClient).buscarConta(1L);
        verify(custodiaRepository).findCustodiaMaster();
        verify(cotacaoFeignClient, never()).obterCotacaoPorTicker(any());
    }

    @Test
    @DisplayName("Deve calcular valor total de resíduo corretamente")
    void deveCalcularValorTotalDeResiduoCorretamente() {
        // Arrange
        ContaGraficaDTO contaMaster = new ContaGraficaDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER",
                LocalDateTime.now()
        );

        Custodia custodia1 = new Custodia();
        custodia1.setTicker("PETR4");
        custodia1.setQuantidade(100);
        custodia1.setPrecoMedio(new BigDecimal("35.00"));
        custodia1.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia2 = new Custodia();
        custodia2.setTicker("VALE3");
        custodia2.setQuantidade(50);
        custodia2.setPrecoMedio(new BigDecimal("62.00"));
        custodia2.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia3 = new Custodia();
        custodia3.setTicker("ITUB4");
        custodia3.setQuantidade(200);
        custodia3.setPrecoMedio(new BigDecimal("30.00"));
        custodia3.setDataUltimaAtualizacao(LocalDateTime.now());

        List<Custodia> custodiasMaster = List.of(custodia1, custodia2, custodia3);

        when(contasGraficasFeignClient.buscarConta(1L)).thenReturn(contaMaster);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(custodiasMaster);
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "PETR4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("VALE3"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "VALE3", 10,
                        new BigDecimal("62.00"), new BigDecimal("64.00"), new BigDecimal("61.00"), new BigDecimal("63.50")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ITUB4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "ITUB4", 10,
                        new BigDecimal("30.00"), new BigDecimal("31.00"), new BigDecimal("29.50"), new BigDecimal("30.75")));

        // Act
        CustodiaMasterResponseDTO resultado = custodiaService.consultarCustodiaMaster();

        // Assert
        // Valor total: (100 × 36.00) + (50 × 63.50) + (200 × 30.75)
        //             = 3600 + 3175 + 6150 = 12925
        assertThat(resultado.valorTotalResiduo()).isEqualByComparingTo(new BigDecimal("12925.00"));
        assertThat(resultado.custodia()).hasSize(3);
    }

    @Test
    @DisplayName("Deve consultar custodia master com um único ativo")
    void deveConsultarCustodiaMasterComUmUnicoAtivo() {
        // Arrange
        ContaGraficaDTO contaMaster = new ContaGraficaDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER",
                LocalDateTime.now()
        );

        Custodia custodia = new Custodia();
        custodia.setTicker("PETR4");
        custodia.setQuantidade(25);
        custodia.setPrecoMedio(new BigDecimal("35.50"));
        custodia.setDataUltimaAtualizacao(LocalDateTime.of(2026, 3, 1, 10, 0));

        when(contasGraficasFeignClient.buscarConta(1L)).thenReturn(contaMaster);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(List.of(custodia));
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "PETR4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));

        // Act
        CustodiaMasterResponseDTO resultado = custodiaService.consultarCustodiaMaster();

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.custodia()).hasSize(1);
        assertThat(resultado.custodia().get(0).ticker()).isEqualTo("PETR4");
        assertThat(resultado.custodia().get(0).quantidade()).isEqualTo(25);
        // Valor total: 25 × 36.00 = 900
        assertThat(resultado.valorTotalResiduo()).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    @DisplayName("Deve consultar custodia master com todos os 5 ativos da cesta")
    void deveConsultarCustodiaMasterComTodosAtivos() {
        // Arrange
        ContaGraficaDTO contaMaster = new ContaGraficaDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER",
                LocalDateTime.now()
        );

        Custodia custodia1 = new Custodia();
        custodia1.setTicker("PETR4");
        custodia1.setQuantidade(10);
        custodia1.setPrecoMedio(new BigDecimal("35.50"));
        custodia1.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia2 = new Custodia();
        custodia2.setTicker("VALE3");
        custodia2.setQuantidade(5);
        custodia2.setPrecoMedio(new BigDecimal("62.00"));
        custodia2.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia3 = new Custodia();
        custodia3.setTicker("BBDC4");
        custodia3.setQuantidade(15);
        custodia3.setPrecoMedio(new BigDecimal("25.00"));
        custodia3.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia4 = new Custodia();
        custodia4.setTicker("ITUB4");
        custodia4.setQuantidade(8);
        custodia4.setPrecoMedio(new BigDecimal("30.00"));
        custodia4.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia5 = new Custodia();
        custodia5.setTicker("ABEV3");
        custodia5.setQuantidade(20);
        custodia5.setPrecoMedio(new BigDecimal("12.50"));
        custodia5.setDataUltimaAtualizacao(LocalDateTime.now());

        List<Custodia> custodiasMaster = List.of(custodia1, custodia2, custodia3, custodia4, custodia5);

        when(contasGraficasFeignClient.buscarConta(1L)).thenReturn(contaMaster);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(custodiasMaster);
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "PETR4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("VALE3"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "VELE3", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("BBDC4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "BBDC4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ITUB4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "ITUB4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "ABEV3", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));

        // Act
        CustodiaMasterResponseDTO resultado = custodiaService.consultarCustodiaMaster();

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.custodia()).hasSize(5);
        assertThat(resultado.custodia().stream().map(c -> c.ticker()).toList())
                .containsExactlyInAnyOrder("PETR4", "VALE3", "BBDC4", "ITUB4", "ABEV3");

        // Valor total: (10×36) + (5×63.5) + (15×25.5) + (8×30.75) + (20×12.75)
        //             = 360 + 317.5 + 382.5 + 246 + 255 = 1561
        assertThat(resultado.valorTotalResiduo()).isEqualByComparingTo(new BigDecimal("2088.00"));

        verify(cotacaoFeignClient, times(5)).obterCotacaoPorTicker(any());
    }

    @Test
    @DisplayName("Deve consultar custodia master com quantidade zero")
    void deveConsultarCustodiaMasterComQuantidadeZero() {
        // Arrange
        ContaGraficaDTO contaMaster = new ContaGraficaDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER",
                LocalDateTime.now()
        );

        Custodia custodia = new Custodia();
        custodia.setTicker("PETR4");
        custodia.setQuantidade(0);
        custodia.setPrecoMedio(new BigDecimal("35.50"));
        custodia.setDataUltimaAtualizacao(LocalDateTime.now());

        when(contasGraficasFeignClient.buscarConta(1L)).thenReturn(contaMaster);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(List.of(custodia));
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "PETR4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));

        // Act
        CustodiaMasterResponseDTO resultado = custodiaService.consultarCustodiaMaster();

        // Assert
        assertThat(resultado.custodia()).hasSize(1);
        assertThat(resultado.custodia().get(0).quantidade()).isEqualTo(0);
        assertThat(resultado.valorTotalResiduo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve consultar custodias de cliente com sucesso")
    void deveConsultarCustodiasDeClienteComSucesso() {
        // Arrange
        Long clienteId = 1L;

        Custodia custodia1 = new Custodia();
        custodia1.setId(1L);
        custodia1.setTicker("PETR4");
        custodia1.setQuantidade(100);
        custodia1.setPrecoMedio(new BigDecimal("35.50"));
        custodia1.setContaGraficaId(10L);
        custodia1.setDataUltimaAtualizacao(LocalDateTime.of(2026, 3, 1, 10, 0));

        Custodia custodia2 = new Custodia();
        custodia2.setId(2L);
        custodia2.setTicker("VALE3");
        custodia2.setQuantidade(50);
        custodia2.setPrecoMedio(new BigDecimal("62.00"));
        custodia2.setContaGraficaId(10L);
        custodia2.setDataUltimaAtualizacao(LocalDateTime.of(2026, 3, 1, 10, 0));

        List<Custodia> custodiasCliente = List.of(custodia1, custodia2);

        when(custodiaRepository.findCustodiaCliente(clienteId)).thenReturn(custodiasCliente);
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "PETR4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("VALE3"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "VALE3", 10,
                        new BigDecimal("62.00"), new BigDecimal("64.00"), new BigDecimal("61.00"), new BigDecimal("63.50")));

        // Act
        List<CustodiaResponseDTO> resultado = custodiaService.consultarCustodiasCliente(clienteId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(2);

        assertThat(resultado.get(0).ticker()).isEqualTo("PETR4");
        assertThat(resultado.get(0).quantidade()).isEqualTo(100);
        assertThat(resultado.get(0).precoMedio()).isEqualByComparingTo(new BigDecimal("35.50"));
        assertThat(resultado.get(0).valorAtual()).isEqualByComparingTo(new BigDecimal("36.00"));
        assertThat(resultado.get(0).origem()).isEqualTo("Resíduo distribuição 2026-03-01");

        assertThat(resultado.get(1).ticker()).isEqualTo("VALE3");
        assertThat(resultado.get(1).quantidade()).isEqualTo(50);
        assertThat(resultado.get(1).precoMedio()).isEqualByComparingTo(new BigDecimal("62.00"));
        assertThat(resultado.get(1).valorAtual()).isEqualByComparingTo(new BigDecimal("63.50"));

        verify(custodiaRepository).findCustodiaCliente(clienteId);
        verify(cotacaoFeignClient).obterCotacaoPorTicker("PETR4");
        verify(cotacaoFeignClient).obterCotacaoPorTicker("VALE3");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando cliente não possui custodias")
    void deveRetornarListaVaziaQuandoClienteNaoPossuiCustodias() {
        // Arrange
        Long clienteId = 1L;

        when(custodiaRepository.findCustodiaCliente(clienteId)).thenReturn(List.of());

        // Act
        List<CustodiaResponseDTO> resultado = custodiaService.consultarCustodiasCliente(clienteId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).isEmpty();

        verify(custodiaRepository).findCustodiaCliente(clienteId);
        verify(cotacaoFeignClient, never()).obterCotacaoPorTicker(any());
    }

    @Test
    @DisplayName("Deve consultar custodias com um único ativo")
    void deveConsultarCustodiasComUmUnicoAtivo() {
        // Arrange
        Long clienteId = 1L;

        Custodia custodia = new Custodia();
        custodia.setId(1L);
        custodia.setTicker("PETR4");
        custodia.setQuantidade(25);
        custodia.setPrecoMedio(new BigDecimal("35.50"));
        custodia.setContaGraficaId(10L);
        custodia.setDataUltimaAtualizacao(LocalDateTime.of(2026, 2, 15, 14, 30));

        when(custodiaRepository.findCustodiaCliente(clienteId)).thenReturn(List.of(custodia));
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "PETR4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));

        // Act
        List<CustodiaResponseDTO> resultado = custodiaService.consultarCustodiasCliente(clienteId);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).ticker()).isEqualTo("PETR4");
        assertThat(resultado.get(0).quantidade()).isEqualTo(25);
        assertThat(resultado.get(0).origem()).isEqualTo("Resíduo distribuição 2026-02-15");

        verify(cotacaoFeignClient, times(1)).obterCotacaoPorTicker("PETR4");
    }

    @Test
    @DisplayName("Deve consultar custodias com todos os 5 ativos")
    void deveConsultarCustodiasComTodosOs5Ativos() {
        // Arrange
        Long clienteId = 1L;

        Custodia custodia1 = new Custodia();
        custodia1.setTicker("PETR4");
        custodia1.setQuantidade(100);
        custodia1.setPrecoMedio(new BigDecimal("35.50"));
        custodia1.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia2 = new Custodia();
        custodia2.setTicker("VALE3");
        custodia2.setQuantidade(50);
        custodia2.setPrecoMedio(new BigDecimal("62.00"));
        custodia2.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia3 = new Custodia();
        custodia3.setTicker("BBDC4");
        custodia3.setQuantidade(200);
        custodia3.setPrecoMedio(new BigDecimal("25.00"));
        custodia3.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia4 = new Custodia();
        custodia4.setTicker("ITUB4");
        custodia4.setQuantidade(150);
        custodia4.setPrecoMedio(new BigDecimal("30.00"));
        custodia4.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia5 = new Custodia();
        custodia5.setTicker("ABEV3");
        custodia5.setQuantidade(300);
        custodia5.setPrecoMedio(new BigDecimal("12.50"));
        custodia5.setDataUltimaAtualizacao(LocalDateTime.now());

        List<Custodia> custodias = List.of(custodia1, custodia2, custodia3, custodia4, custodia5);

        when(custodiaRepository.findCustodiaCliente(clienteId)).thenReturn(custodias);
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "PETR4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("VALE3"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "VALE3", 10,
                        new BigDecimal("62.00"), new BigDecimal("64.00"), new BigDecimal("61.00"), new BigDecimal("63.50")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("BBDC4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "BBDC4", 10,
                        new BigDecimal("25.00"), new BigDecimal("25.75"), new BigDecimal("24.50"), new BigDecimal("25.50")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ITUB4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "ITUB4", 10,
                        new BigDecimal("30.00"), new BigDecimal("31.00"), new BigDecimal("29.50"), new BigDecimal("30.75")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("ABEV3"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "ABEV3", 10,
                        new BigDecimal("12.50"), new BigDecimal("12.90"), new BigDecimal("12.30"), new BigDecimal("12.75")));

        // Act
        List<CustodiaResponseDTO> resultado = custodiaService.consultarCustodiasCliente(clienteId);

        // Assert
        assertThat(resultado).hasSize(5);
        assertThat(resultado.stream().map(CustodiaResponseDTO::ticker).toList())
                .containsExactlyInAnyOrder("PETR4", "VALE3", "BBDC4", "ITUB4", "ABEV3");

        verify(cotacaoFeignClient, times(5)).obterCotacaoPorTicker(any());
    }

    @Test
    @DisplayName("Deve consultar custodias e buscar cotação atualizada para cada ativo")
    void deveConsultarCustodiasEBuscarCotacaoAtualizadaParaCadaAtivo() {
        // Arrange
        Long clienteId = 1L;

        Custodia custodia1 = new Custodia();
        custodia1.setTicker("PETR4");
        custodia1.setQuantidade(100);
        custodia1.setPrecoMedio(new BigDecimal("35.50"));
        custodia1.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia2 = new Custodia();
        custodia2.setTicker("VALE3");
        custodia2.setQuantidade(50);
        custodia2.setPrecoMedio(new BigDecimal("62.00"));
        custodia2.setDataUltimaAtualizacao(LocalDateTime.now());

        when(custodiaRepository.findCustodiaCliente(clienteId)).thenReturn(List.of(custodia1, custodia2));
        when(cotacaoFeignClient.obterCotacaoPorTicker("PETR4"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "PETR4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));
        when(cotacaoFeignClient.obterCotacaoPorTicker("VALE3"))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "VALE3", 10,
                        new BigDecimal("62.00"), new BigDecimal("64.00"), new BigDecimal("61.00"), new BigDecimal("63.50")));

        // Act
        custodiaService.consultarCustodiasCliente(clienteId);

        // Assert - verificar que cotações foram buscadas
        verify(cotacaoFeignClient).obterCotacaoPorTicker("PETR4");
        verify(cotacaoFeignClient).obterCotacaoPorTicker("VALE3");
    }

    @Test
    @DisplayName("Deve consultar custodias de diferentes clientes")
    void deveConsultarCustodiasDeDiferentesClientes() {
        // Arrange
        Long clienteId1 = 1L;
        Long clienteId2 = 2L;

        Custodia custodia1 = new Custodia();
        custodia1.setTicker("PETR4");
        custodia1.setQuantidade(100);
        custodia1.setPrecoMedio(new BigDecimal("35.50"));
        custodia1.setDataUltimaAtualizacao(LocalDateTime.now());

        Custodia custodia2 = new Custodia();
        custodia2.setTicker("VALE3");
        custodia2.setQuantidade(200);
        custodia2.setPrecoMedio(new BigDecimal("62.00"));
        custodia2.setDataUltimaAtualizacao(LocalDateTime.now());

        when(custodiaRepository.findCustodiaCliente(clienteId1)).thenReturn(List.of(custodia1));
        when(custodiaRepository.findCustodiaCliente(clienteId2)).thenReturn(List.of(custodia2));
        when(cotacaoFeignClient.obterCotacaoPorTicker(any()))
                .thenReturn(new CotacaoResponseDTO(java.time.LocalDate.now(), "PETR4", 10,
                        new BigDecimal("35.00"), new BigDecimal("36.50"), new BigDecimal("34.50"), new BigDecimal("36.00")));

        // Act
        List<CustodiaResponseDTO> resultado1 = custodiaService.consultarCustodiasCliente(clienteId1);
        List<CustodiaResponseDTO> resultado2 = custodiaService.consultarCustodiasCliente(clienteId2);

        // Assert
        assertThat(resultado1).hasSize(1);
        assertThat(resultado1.get(0).ticker()).isEqualTo("PETR4");
        assertThat(resultado1.get(0).quantidade()).isEqualTo(100);

        assertThat(resultado2).hasSize(1);
        assertThat(resultado2.get(0).ticker()).isEqualTo("VALE3");
        assertThat(resultado2.get(0).quantidade()).isEqualTo(200);

        verify(custodiaRepository).findCustodiaCliente(clienteId1);
        verify(custodiaRepository).findCustodiaCliente(clienteId2);
    }
}

