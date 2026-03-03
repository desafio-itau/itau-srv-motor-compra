package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.ir.EventoIRSumarioDTO;
import com.itau.srv.motor.compras.dto.historico.HistoricoAportesResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresPorDataResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresResponseDTO;
import com.itau.srv.motor.compras.mapper.HistoricoMapper;
import com.itau.srv.motor.compras.model.EventoIR;
import com.itau.srv.motor.compras.model.enums.TipoIR;
import com.itau.srv.motor.compras.repository.EventoIRRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValoresService - Testes Unitários")
class ValoresServiceTest {

    @Mock
    private EventoIRRepository eventoIRRepository;

    @Mock
    private HistoricoMapper historicoMapper;

    @InjectMocks
    private ValoresService valoresService;

    private Long clienteId;
    private List<EventoIR> eventosInvestidos;
    private List<EventoIR> eventosVendidos;

    @BeforeEach
    void setUp() {
        clienteId = 1L;
        eventosInvestidos = new ArrayList<>();
        eventosVendidos = new ArrayList<>();
    }

    @Test
    @DisplayName("Deve calcular valores quando cliente possui apenas investimentos")
    void deveCalcularValoresQuandoClientePossuiApenasInvestimentos() {
        // Arrange
        EventoIR evento1 = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("1000.00"));
        EventoIR evento2 = criarEventoIR(2L, clienteId, "IR_DEDO_DURO", new BigDecimal("2000.00"));
        EventoIR evento3 = criarEventoIR(3L, clienteId, "IR_DEDO_DURO", new BigDecimal("500.00"));

        eventosInvestidos.add(evento1);
        eventosInvestidos.add(evento2);
        eventosInvestidos.add(evento3);

        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(eventosInvestidos);
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(List.of());

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(resultado.valorVendido()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(eventoIRRepository).findInvestidoPorCliente(clienteId);
        verify(eventoIRRepository).findVendidoPorCliente(clienteId);
    }

    @Test
    @DisplayName("Deve calcular valores quando cliente possui investimentos e vendas")
    void deveCalcularValoresQuandoClientePossuiInvestimentosEVendas() {
        // Arrange
        EventoIR eventoInvestido1 = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("1000.00"));
        EventoIR eventoInvestido2 = criarEventoIR(2L, clienteId, "IR_DEDO_DURO", new BigDecimal("2000.00"));
        EventoIR eventoVendido1 = criarEventoIR(3L, clienteId, "IR_VENDA", new BigDecimal("500.00"));
        EventoIR eventoVendido2 = criarEventoIR(4L, clienteId, "IR_VENDA", new BigDecimal("300.00"));

        eventosInvestidos.add(eventoInvestido1);
        eventosInvestidos.add(eventoInvestido2);
        eventosVendidos.add(eventoVendido1);
        eventosVendidos.add(eventoVendido2);

        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(eventosInvestidos);
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(eventosVendidos);

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(resultado.valorVendido()).isEqualByComparingTo(new BigDecimal("800.00"));

        verify(eventoIRRepository).findInvestidoPorCliente(clienteId);
        verify(eventoIRRepository).findVendidoPorCliente(clienteId);
    }

    @Test
    @DisplayName("Deve retornar zero quando cliente não possui eventos")
    void deveRetornarZeroQuandoClienteNaoPossuiEventos() {
        // Arrange
        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(List.of());
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(List.of());

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.valorVendido()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(eventoIRRepository).findInvestidoPorCliente(clienteId);
        verify(eventoIRRepository).findVendidoPorCliente(clienteId);
    }

    @Test
    @DisplayName("Deve retornar zero de investimento quando lista está vazia")
    void deveRetornarZeroDeInvestimentoQuandoListaEstaVazia() {
        // Arrange
        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(List.of());
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(List.of());

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.valorVendido()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve calcular valores quando cliente possui apenas vendas")
    void deveCalcularValoresQuandoClientePossuiApenasVendas() {
        // Arrange
        EventoIR eventoVendido1 = criarEventoIR(1L, clienteId, "IR_VENDA", new BigDecimal("1500.00"));
        EventoIR eventoVendido2 = criarEventoIR(2L, clienteId, "IR_VENDA", new BigDecimal("2500.00"));

        eventosVendidos.add(eventoVendido1);
        eventosVendidos.add(eventoVendido2);

        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(List.of());
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(eventosVendidos);

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.valorVendido()).isEqualByComparingTo(new BigDecimal("4000.00"));

        verify(eventoIRRepository).findInvestidoPorCliente(clienteId);
        verify(eventoIRRepository).findVendidoPorCliente(clienteId);
    }

    @Test
    @DisplayName("Deve calcular valores com múltiplos eventos de diferentes valores")
    void deveCalcularValoresComMultiplosEventosDeDiferentesValores() {
        // Arrange
        EventoIR evento1 = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("100.50"));
        EventoIR evento2 = criarEventoIR(2L, clienteId, "IR_DEDO_DURO", new BigDecimal("250.75"));
        EventoIR evento3 = criarEventoIR(3L, clienteId, "IR_DEDO_DURO", new BigDecimal("300.25"));
        EventoIR evento4 = criarEventoIR(4L, clienteId, "IR_DEDO_DURO", new BigDecimal("50.00"));

        eventosInvestidos.add(evento1);
        eventosInvestidos.add(evento2);
        eventosInvestidos.add(evento3);
        eventosInvestidos.add(evento4);

        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(eventosInvestidos);
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(List.of());

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(new BigDecimal("701.50"));
        assertThat(resultado.valorVendido()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve calcular valores com um único evento de investimento")
    void deveCalcularValoresComUmUnicoEventoDeInvestimento() {
        // Arrange
        EventoIR evento = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("5000.00"));
        eventosInvestidos.add(evento);

        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(eventosInvestidos);
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(List.of());

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(resultado.valorVendido()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve calcular valores com um único evento de venda")
    void deveCalcularValoresComUmUnicoEventoDeVenda() {
        // Arrange
        EventoIR evento = criarEventoIR(1L, clienteId, "IR_VENDA", new BigDecimal("3000.00"));
        eventosVendidos.add(evento);

        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(List.of());
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(eventosVendidos);

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.valorVendido()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("Deve calcular valores para diferentes clientes")
    void deveCalcularValoresParaDiferentesClientes() {
        // Arrange
        Long clienteId1 = 1L;
        Long clienteId2 = 2L;

        EventoIR evento1 = criarEventoIR(1L, clienteId1, "IR_DEDO_DURO", new BigDecimal("1000.00"));
        EventoIR evento2 = criarEventoIR(2L, clienteId2, "IR_DEDO_DURO", new BigDecimal("2000.00"));

        when(eventoIRRepository.findInvestidoPorCliente(clienteId1)).thenReturn(List.of(evento1));
        when(eventoIRRepository.findVendidoPorCliente(clienteId1)).thenReturn(List.of());
        when(eventoIRRepository.findInvestidoPorCliente(clienteId2)).thenReturn(List.of(evento2));
        when(eventoIRRepository.findVendidoPorCliente(clienteId2)).thenReturn(List.of());

        // Act
        ValoresResponseDTO resultado1 = valoresService.calcularValoresCliente(clienteId1);
        ValoresResponseDTO resultado2 = valoresService.calcularValoresCliente(clienteId2);

        // Assert
        assertThat(resultado1.valorInvestido()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(resultado2.valorInvestido()).isEqualByComparingTo(new BigDecimal("2000.00"));

        verify(eventoIRRepository).findInvestidoPorCliente(clienteId1);
        verify(eventoIRRepository).findInvestidoPorCliente(clienteId2);
    }

    @Test
    @DisplayName("Deve somar corretamente valores decimais")
    void deveSomarCorretamenteValoresDecimais() {
        // Arrange
        EventoIR evento1 = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("100.99"));
        EventoIR evento2 = criarEventoIR(2L, clienteId, "IR_DEDO_DURO", new BigDecimal("200.01"));
        EventoIR eventoVenda = criarEventoIR(3L, clienteId, "IR_VENDA", new BigDecimal("50.50"));

        eventosInvestidos.add(evento1);
        eventosInvestidos.add(evento2);
        eventosVendidos.add(eventoVenda);

        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(eventosInvestidos);
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(eventosVendidos);

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(new BigDecimal("301.00"));
        assertThat(resultado.valorVendido()).isEqualByComparingTo(new BigDecimal("50.50"));
    }

    @Test
    @DisplayName("Deve chamar repository com clienteId correto")
    void deveChamarRepositoryComClienteIdCorreto() {
        // Arrange
        Long clienteIdEspecifico = 999L;

        when(eventoIRRepository.findInvestidoPorCliente(clienteIdEspecifico)).thenReturn(List.of());
        when(eventoIRRepository.findVendidoPorCliente(clienteIdEspecifico)).thenReturn(List.of());

        // Act
        valoresService.calcularValoresCliente(clienteIdEspecifico);

        // Assert
        verify(eventoIRRepository).findInvestidoPorCliente(eq(999L));
        verify(eventoIRRepository).findVendidoPorCliente(eq(999L));
        verify(eventoIRRepository, times(1)).findInvestidoPorCliente(any());
        verify(eventoIRRepository, times(1)).findVendidoPorCliente(any());
    }

    @Test
    @DisplayName("Deve processar valores grandes corretamente")
    void deveProcessarValoresGrandesCorretamente() {
        // Arrange
        EventoIR evento1 = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("999999.99"));
        EventoIR evento2 = criarEventoIR(2L, clienteId, "IR_DEDO_DURO", new BigDecimal("1000000.01"));

        eventosInvestidos.add(evento1);
        eventosInvestidos.add(evento2);

        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(eventosInvestidos);
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(List.of());

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(new BigDecimal("2000000.00"));
    }

    @Test
    @DisplayName("Deve retornar DTO correto quando não há investimentos mas há vendas")
    void deveRetornarDTOCorretoQuandoNaoHaInvestimentosMasHaVendas() {
        // Arrange
        EventoIR eventoVenda = criarEventoIR(1L, clienteId, "IR_VENDA", new BigDecimal("100.00"));
        eventosVendidos.add(eventoVenda);

        when(eventoIRRepository.findInvestidoPorCliente(clienteId)).thenReturn(List.of());
        when(eventoIRRepository.findVendidoPorCliente(clienteId)).thenReturn(eventosVendidos);

        // Act
        ValoresResponseDTO resultado = valoresService.calcularValoresCliente(clienteId);

        // Assert
        assertThat(resultado.valorInvestido()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.valorVendido()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    // Método auxiliar para criar EventoIR
    private EventoIR criarEventoIR(Long id, Long clienteId, String tipo, BigDecimal valorBase) {
        EventoIR evento = new EventoIR();
        evento.setId(id);
        evento.setClienteId(clienteId);
        evento.setTipo(TipoIR.valueOf(tipo));
        evento.setValorBase(valorBase);
        evento.setValorIR(valorBase.multiply(new BigDecimal("0.00005")));
        return evento;
    }

    // Testes para consultarPorClienteEData

    @Test
    @DisplayName("Deve consultar valores por cliente e data com sucesso")
    void deveConsultarValoresPorClienteEDataComSucesso() {
        // Arrange
        Long clienteId = 1L;
        LocalDate data = LocalDate.of(2025, 3, 1);

        EventoIR eventoInvestido = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("1000.00"));
        EventoIR eventoVendido = criarEventoIR(2L, clienteId, "IR_VENDA", new BigDecimal("500.00"));

        when(eventoIRRepository.findInvestidoPorClienteEData(clienteId, data))
                .thenReturn(List.of(eventoInvestido));
        when(eventoIRRepository.findVendidoPorClienteEData(clienteId, data))
                .thenReturn(List.of(eventoVendido));

        // Act
        ValoresPorDataResponseDTO resultado = valoresService.consultarPorClienteEData(clienteId, data);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.dataEvento()).isEqualTo(data);
        assertThat(resultado.valores().valorInvestido()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(resultado.valores().valorVendido()).isEqualByComparingTo(new BigDecimal("500.00"));

        verify(eventoIRRepository).findInvestidoPorClienteEData(clienteId, data);
        verify(eventoIRRepository).findVendidoPorClienteEData(clienteId, data);
    }

    @Test
    @DisplayName("Deve retornar valores zerados quando não há eventos na data")
    void deveRetornarValoresZeradosQuandoNaoHaEventosNaData() {
        // Arrange
        Long clienteId = 1L;
        LocalDate data = LocalDate.of(2025, 3, 1);

        when(eventoIRRepository.findInvestidoPorClienteEData(clienteId, data)).thenReturn(List.of());
        when(eventoIRRepository.findVendidoPorClienteEData(clienteId, data)).thenReturn(List.of());

        // Act
        ValoresPorDataResponseDTO resultado = valoresService.consultarPorClienteEData(clienteId, data);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.dataEvento()).isEqualTo(data);
        assertThat(resultado.valores().valorInvestido()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.valores().valorVendido()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve consultar valores com múltiplos eventos na mesma data")
    void deveConsultarValoresComMultiplosEventosNaMesmaData() {
        // Arrange
        Long clienteId = 1L;
        LocalDate data = LocalDate.of(2025, 3, 1);

        EventoIR evento1 = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("1000.00"));
        EventoIR evento2 = criarEventoIR(2L, clienteId, "IR_DEDO_DURO", new BigDecimal("2000.00"));
        EventoIR evento3 = criarEventoIR(3L, clienteId, "IR_DEDO_DURO", new BigDecimal("500.00"));

        EventoIR eventoVenda1 = criarEventoIR(4L, clienteId, "IR_VENDA", new BigDecimal("300.00"));
        EventoIR eventoVenda2 = criarEventoIR(5L, clienteId, "IR_VENDA", new BigDecimal("200.00"));

        when(eventoIRRepository.findInvestidoPorClienteEData(clienteId, data))
                .thenReturn(List.of(evento1, evento2, evento3));
        when(eventoIRRepository.findVendidoPorClienteEData(clienteId, data))
                .thenReturn(List.of(eventoVenda1, eventoVenda2));

        // Act
        ValoresPorDataResponseDTO resultado = valoresService.consultarPorClienteEData(clienteId, data);

        // Assert
        assertThat(resultado.valores().valorInvestido()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(resultado.valores().valorVendido()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Deve consultar valores apenas com investimentos na data")
    void deveConsultarValoresApenasComInvestimentosNaData() {
        // Arrange
        Long clienteId = 1L;
        LocalDate data = LocalDate.of(2025, 3, 1);

        EventoIR evento = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("1500.00"));

        when(eventoIRRepository.findInvestidoPorClienteEData(clienteId, data)).thenReturn(List.of(evento));
        when(eventoIRRepository.findVendidoPorClienteEData(clienteId, data)).thenReturn(List.of());

        // Act
        ValoresPorDataResponseDTO resultado = valoresService.consultarPorClienteEData(clienteId, data);

        // Assert
        assertThat(resultado.valores().valorInvestido()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(resultado.valores().valorVendido()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve consultar valores apenas com vendas na data")
    void deveConsultarValoresApenasComVendasNaData() {
        // Arrange
        Long clienteId = 1L;
        LocalDate data = LocalDate.of(2025, 3, 1);

        EventoIR eventoVenda = criarEventoIR(1L, clienteId, "IR_VENDA", new BigDecimal("800.00"));

        when(eventoIRRepository.findInvestidoPorClienteEData(clienteId, data)).thenReturn(List.of());
        when(eventoIRRepository.findVendidoPorClienteEData(clienteId, data)).thenReturn(List.of(eventoVenda));

        // Act
        ValoresPorDataResponseDTO resultado = valoresService.consultarPorClienteEData(clienteId, data);

        // Assert
        assertThat(resultado.valores().valorInvestido()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.valores().valorVendido()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    @DisplayName("Deve consultar valores para diferentes datas")
    void deveConsultarValoresParaDiferentesDatas() {
        // Arrange
        Long clienteId = 1L;
        LocalDate data1 = LocalDate.of(2025, 3, 1);
        LocalDate data2 = LocalDate.of(2025, 3, 15);

        EventoIR evento1 = criarEventoIR(1L, clienteId, "IR_DEDO_DURO", new BigDecimal("1000.00"));
        EventoIR evento2 = criarEventoIR(2L, clienteId, "IR_DEDO_DURO", new BigDecimal("2000.00"));

        when(eventoIRRepository.findInvestidoPorClienteEData(clienteId, data1))
                .thenReturn(List.of(evento1));
        when(eventoIRRepository.findVendidoPorClienteEData(clienteId, data1))
                .thenReturn(List.of());
        when(eventoIRRepository.findInvestidoPorClienteEData(clienteId, data2))
                .thenReturn(List.of(evento2));
        when(eventoIRRepository.findVendidoPorClienteEData(clienteId, data2))
                .thenReturn(List.of());

        // Act
        ValoresPorDataResponseDTO resultado1 = valoresService.consultarPorClienteEData(clienteId, data1);
        ValoresPorDataResponseDTO resultado2 = valoresService.consultarPorClienteEData(clienteId, data2);

        // Assert
        assertThat(resultado1.dataEvento()).isEqualTo(data1);
        assertThat(resultado1.valores().valorInvestido()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(resultado2.dataEvento()).isEqualTo(data2);
        assertThat(resultado2.valores().valorInvestido()).isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    // Testes para consultarHistoricoAportes

    @Test
    @DisplayName("Deve consultar histórico de aportes com sucesso")
    void deveConsultarHistoricoAportesComSucesso() {
        // Arrange
        Long clienteId = 1L;

        EventoIRSumarioDTO sumario1 = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 5),
                new BigDecimal("1000.00")
        );

        EventoIRSumarioDTO sumario2 = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 15),
                new BigDecimal("2000.00")
        );

        HistoricoAportesResponseDTO historico1 = new HistoricoAportesResponseDTO(
                LocalDate.of(2025, 3, 5),
                new BigDecimal("1000.00"),
                "1/3"
        );

        HistoricoAportesResponseDTO historico2 = new HistoricoAportesResponseDTO(
                LocalDate.of(2025, 3, 15),
                new BigDecimal("2000.00"),
                "2/3"
        );

        when(eventoIRRepository.findSumarioEventosPorDataECliente(clienteId))
                .thenReturn(List.of(sumario1, sumario2));
        when(historicoMapper.mapearParaHistoricoAportes(sumario1)).thenReturn(historico1);
        when(historicoMapper.mapearParaHistoricoAportes(sumario2)).thenReturn(historico2);

        // Act
        List<HistoricoAportesResponseDTO> resultado = valoresService.consultarHistoricoAportes(clienteId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).data()).isEqualTo(LocalDate.of(2025, 3, 5));
        assertThat(resultado.get(0).valor()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(resultado.get(0).parcela()).isEqualTo("1/3");
        assertThat(resultado.get(1).data()).isEqualTo(LocalDate.of(2025, 3, 15));
        assertThat(resultado.get(1).valor()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(resultado.get(1).parcela()).isEqualTo("2/3");

        verify(eventoIRRepository).findSumarioEventosPorDataECliente(clienteId);
        verify(historicoMapper, times(2)).mapearParaHistoricoAportes(any(EventoIRSumarioDTO.class));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há histórico de aportes")
    void deveRetornarListaVaziaQuandoNaoHaHistoricoAportes() {
        // Arrange
        Long clienteId = 1L;

        when(eventoIRRepository.findSumarioEventosPorDataECliente(clienteId)).thenReturn(List.of());

        // Act
        List<HistoricoAportesResponseDTO> resultado = valoresService.consultarHistoricoAportes(clienteId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).isEmpty();

        verify(eventoIRRepository).findSumarioEventosPorDataECliente(clienteId);
        verify(historicoMapper, never()).mapearParaHistoricoAportes(any());
    }

    @Test
    @DisplayName("Deve consultar histórico com um único aporte")
    void deveConsultarHistoricoComUmUnicoAporte() {
        // Arrange
        Long clienteId = 1L;

        EventoIRSumarioDTO sumario = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 25),
                new BigDecimal("1500.00")
        );

        HistoricoAportesResponseDTO historico = new HistoricoAportesResponseDTO(
                LocalDate.of(2025, 3, 25),
                new BigDecimal("1500.00"),
                "3/3"
        );

        when(eventoIRRepository.findSumarioEventosPorDataECliente(clienteId))
                .thenReturn(List.of(sumario));
        when(historicoMapper.mapearParaHistoricoAportes(sumario)).thenReturn(historico);

        // Act
        List<HistoricoAportesResponseDTO> resultado = valoresService.consultarHistoricoAportes(clienteId);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).parcela()).isEqualTo("3/3");
    }

    @Test
    @DisplayName("Deve consultar histórico com múltiplos aportes")
    void deveConsultarHistoricoComMultiplosAportes() {
        // Arrange
        Long clienteId = 1L;

        List<EventoIRSumarioDTO> sumarios = List.of(
                new EventoIRSumarioDTO(LocalDate.of(2025, 1, 5), new BigDecimal("1000.00")),
                new EventoIRSumarioDTO(LocalDate.of(2025, 1, 15), new BigDecimal("1000.00")),
                new EventoIRSumarioDTO(LocalDate.of(2025, 1, 25), new BigDecimal("1000.00")),
                new EventoIRSumarioDTO(LocalDate.of(2025, 2, 5), new BigDecimal("1000.00")),
                new EventoIRSumarioDTO(LocalDate.of(2025, 2, 15), new BigDecimal("1000.00"))
        );

        when(eventoIRRepository.findSumarioEventosPorDataECliente(clienteId)).thenReturn(sumarios);

        for (EventoIRSumarioDTO sumario : sumarios) {
            when(historicoMapper.mapearParaHistoricoAportes(sumario))
                    .thenReturn(new HistoricoAportesResponseDTO(
                            sumario.data(),
                            sumario.valorBaseTotalCompras(),
                            "1/3"
                    ));
        }

        // Act
        List<HistoricoAportesResponseDTO> resultado = valoresService.consultarHistoricoAportes(clienteId);

        // Assert
        assertThat(resultado).hasSize(5);
        verify(historicoMapper, times(5)).mapearParaHistoricoAportes(any(EventoIRSumarioDTO.class));
    }

    @Test
    @DisplayName("Deve chamar repository com clienteId correto para histórico")
    void deveChamarRepositoryComClienteIdCorretoParaHistorico() {
        // Arrange
        Long clienteId = 999L;

        when(eventoIRRepository.findSumarioEventosPorDataECliente(clienteId)).thenReturn(List.of());

        // Act
        valoresService.consultarHistoricoAportes(clienteId);

        // Assert
        verify(eventoIRRepository).findSumarioEventosPorDataECliente(eq(999L));
        verify(eventoIRRepository, times(1)).findSumarioEventosPorDataECliente(any());
    }
}


