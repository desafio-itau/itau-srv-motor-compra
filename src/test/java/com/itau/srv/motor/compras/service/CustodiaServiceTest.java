package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.agrupamento.AgrupamentoResponseDTO;
import com.itau.srv.motor.compras.dto.agrupamento.ClienteAgrupamentoDTO;
import com.itau.srv.motor.compras.dto.distribuicao.DistribuicaoResponseDTO;
import com.itau.srv.motor.compras.dto.ordemcompra.CalcularQuantidadeAtivoResponse;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @InjectMocks
    private CustodiaService custodiaService;

    private AgrupamentoResponseDTO agrupamento;
    private CalcularQuantidadeAtivoResponse calculoAtivos;
    private List<ClienteAgrupamentoDTO> clientes;
    private List<OrdemCompra> ordensCompra;

    @BeforeEach
    void setUp() {
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
}

