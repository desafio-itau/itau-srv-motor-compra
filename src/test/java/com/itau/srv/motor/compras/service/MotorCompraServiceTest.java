package com.itau.srv.motor.compras.service;

import com.itau.common.library.exception.ConflitoException;
import com.itau.srv.motor.compras.dto.agrupamento.AgrupamentoResponseDTO;
import com.itau.srv.motor.compras.dto.agrupamento.ClienteAgrupamentoDTO;
import com.itau.srv.motor.compras.dto.ativo.AtivoResponseDTO;
import com.itau.srv.motor.compras.dto.distribuicao.DistribuicaoResponseDTO;
import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraRequestDTO;
import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraResponseDTO;
import com.itau.srv.motor.compras.dto.ordemcompra.CalcularQuantidadeAtivoResponse;
import com.itau.srv.motor.compras.dto.ordemcompra.OrdemCompraResponseDTO;
import com.itau.srv.motor.compras.feign.ClientesFeignClient;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import com.itau.srv.motor.compras.repository.OrdemCompraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MotorCompraService - Testes Unitários")
class MotorCompraServiceTest {

    @Mock
    private CustodiaRepository custodiaRepository;

    @Mock
    private CustodiaService custodiaService;

    @Mock
    private QuantidadeAtivoService quantidadeAtivoService;

    @Mock
    private ClienteService clienteService;

    @Mock
    private OrdemCompraRepository ordemCompraRepository;

    @Mock
    private ClientesFeignClient clientesFeignClient;

    @InjectMocks
    private MotorCompraService motorCompraService;

    private MotorCompraRequestDTO request;
    private AgrupamentoResponseDTO agrupamento;
    private CalcularQuantidadeAtivoResponse calculoAtivos;
    private List<DistribuicaoResponseDTO> distribuicoes;
    private LocalDate dataReferencia;

    @BeforeEach
    void setUp() {
        dataReferencia = LocalDate.of(2026, 3, 3); // Segunda-feira
        request = new MotorCompraRequestDTO(dataReferencia);

        // Configurar agrupamento
        List<ClienteAgrupamentoDTO> clientes = List.of(
                new ClienteAgrupamentoDTO(1L, "Cliente A", "12345678901", 1L, new BigDecimal("1000.00")),
                new ClienteAgrupamentoDTO(2L, "Cliente B", "98765432100", 2L, new BigDecimal("2000.00"))
        );
        agrupamento = new AgrupamentoResponseDTO(new BigDecimal("3000.00"), clientes);

        // Configurar cálculo de ativos
        List<OrdemCompraResponseDTO> ordensDTO = List.of(
                new OrdemCompraResponseDTO("PETR4", 100, Collections.emptyList(), new BigDecimal("35.00"), new BigDecimal("3500.00")),
                new OrdemCompraResponseDTO("VALE3", 50, Collections.emptyList(), new BigDecimal("62.00"), new BigDecimal("3100.00"))
        );
        calculoAtivos = new CalcularQuantidadeAtivoResponse(Collections.emptyList(), ordensDTO);

        // Configurar distribuições
        List<AtivoResponseDTO> ativosCliente1 = List.of(
                new AtivoResponseDTO("PETR4", 33),
                new AtivoResponseDTO("VALE3", 16)
        );
        List<AtivoResponseDTO> ativosCliente2 = List.of(
                new AtivoResponseDTO("PETR4", 67),
                new AtivoResponseDTO("VALE3", 34)
        );
        distribuicoes = List.of(
                new DistribuicaoResponseDTO(1L, "Cliente A", new BigDecimal("1000.00"), ativosCliente1),
                new DistribuicaoResponseDTO(2L, "Cliente B", new BigDecimal("2000.00"), ativosCliente2)
        );

        dataReferencia = LocalDate.now();
    }

    @Test
    @DisplayName("Deve executar motor de compras com sucesso em dia útil")
    void deveExecutarMotorDeComprasComSucessoEmDiaUtil() {
        // Arrange
        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);
        when(clienteService.agruparClientes()).thenReturn(agrupamento);
        when(quantidadeAtivoService.calcularQuantidadePorAtivo(any(), any())).thenReturn(calculoAtivos);
        when(custodiaService.distribuirCustodias(any(), any())).thenReturn(distribuicoes);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        MotorCompraResponseDTO resultado = motorCompraService.acionarMotorCompra(request);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.totalClientes()).isEqualTo(2);
        assertThat(resultado.totalConsolidado()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(resultado.ordensCompra()).hasSize(2);
        assertThat(resultado.distribuicoes()).hasSize(2);
        assertThat(resultado.eventosIRPublicados()).isEqualTo(4); // 2 clientes × 2 ativos

        verify(clienteService, times(1)).agruparClientes();
        verify(quantidadeAtivoService, times(1)).calcularQuantidadePorAtivo(any(), any());
        verify(custodiaService, times(1)).distribuirCustodias(any(), any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando compra já foi executada na data")
    void deveLancarExcecaoQuandoCompraJaFoiExecutadaNaData() {
        // Arrange
        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> motorCompraService.acionarMotorCompra(request))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("COMPRA_JA_EXECUTADA");

        verify(clienteService, never()).agruparClientes();
        verify(quantidadeAtivoService, never()).calcularQuantidadePorAtivo(any(), any());
    }

    @Test
    @DisplayName("Deve validar dia útil - Segunda-feira")
    void deveValidarDiaUtilSegundaFeira() {
        // Arrange
        LocalDate segundaFeira = LocalDate.of(2026, 3, 2);
        MotorCompraRequestDTO requestSegunda = new MotorCompraRequestDTO(segundaFeira);

        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);
        when(clienteService.agruparClientes()).thenReturn(agrupamento);
        when(quantidadeAtivoService.calcularQuantidadePorAtivo(any(), any())).thenReturn(calculoAtivos);
        when(custodiaService.distribuirCustodias(any(), any())).thenReturn(distribuicoes);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        MotorCompraResponseDTO resultado = motorCompraService.acionarMotorCompra(requestSegunda);

        // Assert
        assertThat(resultado).isNotNull();
        verify(clienteService, times(1)).agruparClientes();
    }

    @Test
    @DisplayName("Deve validar dia útil - Sexta-feira")
    void deveValidarDiaUtilSextaFeira() {
        // Arrange
        LocalDate sextaFeira = LocalDate.of(2026, 2, 27);
        MotorCompraRequestDTO requestSexta = new MotorCompraRequestDTO(sextaFeira);

        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);
        when(clienteService.agruparClientes()).thenReturn(agrupamento);
        when(quantidadeAtivoService.calcularQuantidadePorAtivo(any(), any())).thenReturn(calculoAtivos);
        when(custodiaService.distribuirCustodias(any(), any())).thenReturn(distribuicoes);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        MotorCompraResponseDTO resultado = motorCompraService.acionarMotorCompra(requestSexta);

        // Assert
        assertThat(resultado).isNotNull();
        verify(clienteService, times(1)).agruparClientes();
    }

    @Test
    @DisplayName("Deve lançar exceção para sábado")
    void deveLancarExcecaoParaSabado() {
        // Arrange
        LocalDate sabado = LocalDate.of(2026, 2, 28);
        assertThat(sabado.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        MotorCompraRequestDTO requestSabado = new MotorCompraRequestDTO(sabado);

        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> motorCompraService.acionarMotorCompra(requestSabado))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Nosso sistema de motor de compras só funciona em dias úteis");

        verify(clienteService, never()).agruparClientes();
    }

    @Test
    @DisplayName("Deve lançar exceção para domingo")
    void deveLancarExcecaoParaDomingo() {
        // Arrange
        LocalDate domingo = LocalDate.of(2026, 3, 1);
        assertThat(domingo.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        MotorCompraRequestDTO requestDomingo = new MotorCompraRequestDTO(domingo);

        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> motorCompraService.acionarMotorCompra(requestDomingo))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Nosso sistema de motor de compras só funciona em dias úteis");

        verify(clienteService, never()).agruparClientes();
    }

    @Test
    @DisplayName("Deve calcular corretamente total de eventos IR publicados")
    void deveCalcularCorretamenteTotalDeEventosIRPublicados() {
        // Arrange
        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);
        when(clienteService.agruparClientes()).thenReturn(agrupamento);
        when(quantidadeAtivoService.calcularQuantidadePorAtivo(any(), any())).thenReturn(calculoAtivos);
        when(custodiaService.distribuirCustodias(any(), any())).thenReturn(distribuicoes);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        MotorCompraResponseDTO resultado = motorCompraService.acionarMotorCompra(request);

        // Assert
        // Cliente A: 2 ativos + Cliente B: 2 ativos = 4 eventos
        assertThat(resultado.eventosIRPublicados()).isEqualTo(4);
    }

    @Test
    @DisplayName("Deve incluir resíduos na resposta quando existirem")
    void deveIncluirResiduosNaRespostaQuandoExistirem() {
        // Arrange
        List<Custodia> residuosMaster = new ArrayList<>();

        Custodia residuoPETR4 = new Custodia();
        residuoPETR4.setTicker("PETR4");
        residuoPETR4.setQuantidade(3);
        residuosMaster.add(residuoPETR4);

        Custodia residuoVALE3 = new Custodia();
        residuoVALE3.setTicker("VALE3");
        residuoVALE3.setQuantidade(1);
        residuosMaster.add(residuoVALE3);

        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);
        when(clienteService.agruparClientes()).thenReturn(agrupamento);
        when(quantidadeAtivoService.calcularQuantidadePorAtivo(any(), any())).thenReturn(calculoAtivos);
        when(custodiaService.distribuirCustodias(any(), any())).thenReturn(distribuicoes);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(residuosMaster);

        // Act
        MotorCompraResponseDTO resultado = motorCompraService.acionarMotorCompra(request);

        // Assert
        assertThat(resultado.residuosCustMaster()).hasSize(2);
        assertThat(resultado.residuosCustMaster().get(0).ticker()).isEqualTo("PETR4");
        assertThat(resultado.residuosCustMaster().get(0).quantidade()).isEqualTo(3);
        assertThat(resultado.residuosCustMaster().get(1).ticker()).isEqualTo("VALE3");
        assertThat(resultado.residuosCustMaster().get(1).quantidade()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve retornar lista vazia de resíduos quando não houver")
    void deveRetornarListaVaziaDeResiduosQuandoNaoHouver() {
        // Arrange
        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);
        when(clienteService.agruparClientes()).thenReturn(agrupamento);
        when(quantidadeAtivoService.calcularQuantidadePorAtivo(any(), any())).thenReturn(calculoAtivos);
        when(custodiaService.distribuirCustodias(any(), any())).thenReturn(distribuicoes);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        MotorCompraResponseDTO resultado = motorCompraService.acionarMotorCompra(request);

        // Assert
        assertThat(resultado.residuosCustMaster()).isEmpty();
    }

    @Test
    @DisplayName("Deve incluir mensagem de sucesso na resposta")
    void deveIncluirMensagemDeSucessoNaResposta() {
        // Arrange
        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);
        when(clienteService.agruparClientes()).thenReturn(agrupamento);
        when(quantidadeAtivoService.calcularQuantidadePorAtivo(any(), any())).thenReturn(calculoAtivos);
        when(custodiaService.distribuirCustodias(any(), any())).thenReturn(distribuicoes);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        MotorCompraResponseDTO resultado = motorCompraService.acionarMotorCompra(request);

        // Assert
        assertThat(resultado.mensagem()).isEqualTo("Compra programada executada com sucesso para 2 clientes.");
    }

    @Test
    @DisplayName("Deve incluir data de execução na resposta")
    void deveIncluirDataDeExecucaoNaResposta() {
        // Arrange
        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);
        when(clienteService.agruparClientes()).thenReturn(agrupamento);
        when(quantidadeAtivoService.calcularQuantidadePorAtivo(any(), any())).thenReturn(calculoAtivos);
        when(custodiaService.distribuirCustodias(any(), any())).thenReturn(distribuicoes);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        MotorCompraResponseDTO resultado = motorCompraService.acionarMotorCompra(request);

        // Assert
        assertThat(resultado.dataExecucao()).isNotNull();
        assertThat(resultado.dataExecucao().toLocalDate()).isEqualTo(request.dataReferencia());
    }

    @Test
    @DisplayName("Deve processar compra com um único cliente")
    void deveProcessarCompraComUmUnicoCliente() {
        // Arrange
        List<ClienteAgrupamentoDTO> umCliente = List.of(
                new ClienteAgrupamentoDTO(1L, "Cliente Único", "12345678901", 1L, new BigDecimal("1000.00"))
        );
        AgrupamentoResponseDTO agrupamentoUnico = new AgrupamentoResponseDTO(new BigDecimal("1000.00"), umCliente);

        List<AtivoResponseDTO> ativos = List.of(new AtivoResponseDTO("PETR4", 100));
        List<DistribuicaoResponseDTO> distribuicoesUnicas = List.of(
                new DistribuicaoResponseDTO(1L, "Cliente Único", new BigDecimal("1000.00"), ativos)
        );

        when(ordemCompraRepository.existsByDataExecucao(any())).thenReturn(false);
        when(clienteService.agruparClientes()).thenReturn(agrupamentoUnico);
        when(quantidadeAtivoService.calcularQuantidadePorAtivo(any(), any())).thenReturn(calculoAtivos);
        when(custodiaService.distribuirCustodias(any(), any())).thenReturn(distribuicoesUnicas);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());

        // Act
        MotorCompraResponseDTO resultado = motorCompraService.acionarMotorCompra(request);

        // Assert
        assertThat(resultado.totalClientes()).isEqualTo(1);
        assertThat(resultado.distribuicoes()).hasSize(1);
        assertThat(resultado.eventosIRPublicados()).isEqualTo(1);
        assertThat(resultado.mensagem()).contains("1 clientes");
    }
}

