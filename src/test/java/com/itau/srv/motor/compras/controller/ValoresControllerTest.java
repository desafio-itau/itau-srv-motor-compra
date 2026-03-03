package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.historico.HistoricoAportesResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresPorDataResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresResponseDTO;
import com.itau.srv.motor.compras.service.ValoresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValoresController - Testes Unitários")
class ValoresControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ValoresService valoresService;

    @InjectMocks
    private ValoresController valoresController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(valoresController).build();
    }

    @Test
    @DisplayName("Deve obter valores por cliente com sucesso")
    void deveObterValoresPorClienteComSucesso() throws Exception {
        Long clienteId = 1L;
        ValoresResponseDTO response = new ValoresResponseDTO(
                new BigDecimal("3500.00"),
                new BigDecimal("800.00")
        );

        when(valoresService.calcularValoresCliente(clienteId)).thenReturn(response);

        mockMvc.perform(get("/api/valores/{clienteId}", clienteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valorInvestido").value(3500.00))
                .andExpect(jsonPath("$.valorVendido").value(800.00));

        verify(valoresService).calcularValoresCliente(clienteId);
    }

    @Test
    @DisplayName("Deve retornar valores zerados")
    void deveRetornarValoresZerados() throws Exception {
        Long clienteId = 1L;
        ValoresResponseDTO response = new ValoresResponseDTO(BigDecimal.ZERO, BigDecimal.ZERO);

        when(valoresService.calcularValoresCliente(clienteId)).thenReturn(response);

        mockMvc.perform(get("/api/valores/{clienteId}", clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorInvestido").value(0))
                .andExpect(jsonPath("$.valorVendido").value(0));
    }

    @Test
    @DisplayName("Deve chamar service corretamente")
    void deveChamarServiceCorretamente() throws Exception {
        Long clienteId = 42L;
        ValoresResponseDTO response = new ValoresResponseDTO(BigDecimal.ZERO, BigDecimal.ZERO);

        when(valoresService.calcularValoresCliente(clienteId)).thenReturn(response);

        mockMvc.perform(get("/api/valores/{clienteId}", clienteId))
                .andExpect(status().isOk());

        verify(valoresService).calcularValoresCliente(eq(42L));
        verify(valoresService, times(1)).calcularValoresCliente(any());
    }

    // Testes para consultarPorClienteEData

    @Test
    @DisplayName("Deve obter valores por cliente e data com sucesso")
    void deveObterValoresPorClienteEDataComSucesso() throws Exception {
        // Arrange
        Long clienteId = 1L;
        LocalDate data = LocalDate.of(2025, 3, 1);

        ValoresPorDataResponseDTO response = new ValoresPorDataResponseDTO(
                data,
                new ValoresResponseDTO(
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")
                )
        );

        when(valoresService.consultarPorClienteEData(clienteId, data)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/valores")
                        .param("clienteId", "1")
                        .param("data", "2025-03-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valores.valorInvestido").value(1000.00))
                .andExpect(jsonPath("$.valores.valorVendido").value(500.00));

        verify(valoresService).consultarPorClienteEData(clienteId, data);
    }

    @Test
    @DisplayName("Deve obter valores zerados por cliente e data")
    void deveObterValoresZeradosPorClienteEData() throws Exception {
        // Arrange
        Long clienteId = 1L;
        LocalDate data = LocalDate.of(2025, 3, 1);

        ValoresPorDataResponseDTO response = new ValoresPorDataResponseDTO(
                data,
                new ValoresResponseDTO(BigDecimal.ZERO, BigDecimal.ZERO)
        );

        when(valoresService.consultarPorClienteEData(clienteId, data)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/valores")
                        .param("clienteId", "1")
                        .param("data", "2025-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valores.valorInvestido").value(0))
                .andExpect(jsonPath("$.valores.valorVendido").value(0));
    }

    @Test
    @DisplayName("Deve consultar valores para diferentes datas")
    void deveConsultarValoresParaDiferentesDatas() throws Exception {
        // Arrange
        Long clienteId = 1L;
        LocalDate data1 = LocalDate.of(2025, 3, 1);
        LocalDate data2 = LocalDate.of(2025, 3, 15);

        ValoresPorDataResponseDTO response1 = new ValoresPorDataResponseDTO(
                data1,
                new ValoresResponseDTO(new BigDecimal("1000.00"), BigDecimal.ZERO)
        );

        ValoresPorDataResponseDTO response2 = new ValoresPorDataResponseDTO(
                data2,
                new ValoresResponseDTO(new BigDecimal("2000.00"), BigDecimal.ZERO)
        );

        when(valoresService.consultarPorClienteEData(clienteId, data1)).thenReturn(response1);
        when(valoresService.consultarPorClienteEData(clienteId, data2)).thenReturn(response2);

        // Act & Assert - Data 1
        mockMvc.perform(get("/api/valores")
                        .param("clienteId", "1")
                        .param("data", "2025-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valores.valorInvestido").value(1000.00));

        // Act & Assert - Data 2
        mockMvc.perform(get("/api/valores")
                        .param("clienteId", "1")
                        .param("data", "2025-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valores.valorInvestido").value(2000.00));

        verify(valoresService).consultarPorClienteEData(clienteId, data1);
        verify(valoresService).consultarPorClienteEData(clienteId, data2);
    }

    // Testes para consultarHistoricoAportes

    @Test
    @DisplayName("Deve consultar histórico de aportes com sucesso")
    void deveConsultarHistoricoAportesComSucesso() throws Exception {
        // Arrange
        Long clienteId = 1L;

        List<HistoricoAportesResponseDTO> historicos = List.of(
                new HistoricoAportesResponseDTO(
                        LocalDate.of(2025, 3, 5),
                        new BigDecimal("1000.00"),
                        "1/3"
                ),
                new HistoricoAportesResponseDTO(
                        LocalDate.of(2025, 3, 15),
                        new BigDecimal("1000.00"),
                        "2/3"
                ),
                new HistoricoAportesResponseDTO(
                        LocalDate.of(2025, 3, 25),
                        new BigDecimal("1000.00"),
                        "3/3"
                )
        );

        when(valoresService.consultarHistoricoAportes(clienteId)).thenReturn(historicos);

        // Act & Assert
        mockMvc.perform(get("/api/valores/historico/{clienteId}", clienteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].data[0]").value(2025))
                .andExpect(jsonPath("$[0].data[1]").value(3))
                .andExpect(jsonPath("$[0].data[2]").value(5))
                .andExpect(jsonPath("$[0].valor").value(1000.00))
                .andExpect(jsonPath("$[0].parcela").value("1/3"))
                .andExpect(jsonPath("$[1].data[0]").value(2025))
                .andExpect(jsonPath("$[1].data[1]").value(3))
                .andExpect(jsonPath("$[1].data[2]").value(15))
                .andExpect(jsonPath("$[1].parcela").value("2/3"))
                .andExpect(jsonPath("$[2].data[0]").value(2025))
                .andExpect(jsonPath("$[2].data[1]").value(3))
                .andExpect(jsonPath("$[2].data[2]").value(25))
                .andExpect(jsonPath("$[2].parcela").value("3/3"));

        verify(valoresService).consultarHistoricoAportes(clienteId);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há histórico de aportes")
    void deveRetornarListaVaziaQuandoNaoHaHistorico() throws Exception {
        // Arrange
        Long clienteId = 1L;

        when(valoresService.consultarHistoricoAportes(clienteId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/valores/historico/{clienteId}", clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(valoresService).consultarHistoricoAportes(clienteId);
    }

    @Test
    @DisplayName("Deve consultar histórico com um único aporte")
    void deveConsultarHistoricoComUmUnicoAporte() throws Exception {
        // Arrange
        Long clienteId = 1L;

        List<HistoricoAportesResponseDTO> historicos = List.of(
                new HistoricoAportesResponseDTO(
                        LocalDate.of(2025, 3, 5),
                        new BigDecimal("1500.00"),
                        "1/3"
                )
        );

        when(valoresService.consultarHistoricoAportes(clienteId)).thenReturn(historicos);

        // Act & Assert
        mockMvc.perform(get("/api/valores/historico/{clienteId}", clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].valor").value(1500.00));
    }

    @Test
    @DisplayName("Deve chamar service com clienteId correto para histórico")
    void deveChamarServiceComClienteIdCorretoParaHistorico() throws Exception {
        // Arrange
        Long clienteId = 999L;

        when(valoresService.consultarHistoricoAportes(clienteId)).thenReturn(List.of());

        // Act
        mockMvc.perform(get("/api/valores/historico/{clienteId}", clienteId))
                .andExpect(status().isOk());

        // Assert
        verify(valoresService).consultarHistoricoAportes(eq(999L));
        verify(valoresService, times(1)).consultarHistoricoAportes(any());
    }

    @Test
    @DisplayName("Deve retornar parcelas corretas para dias diferentes do mês")
    void deveRetornarParcelasCorretasParaDiasDiferentesDoMes() throws Exception {
        // Arrange
        Long clienteId = 1L;

        List<HistoricoAportesResponseDTO> historicos = List.of(
                new HistoricoAportesResponseDTO(LocalDate.of(2025, 1, 5), new BigDecimal("1000.00"), "1/3"),
                new HistoricoAportesResponseDTO(LocalDate.of(2025, 1, 15), new BigDecimal("1000.00"), "2/3"),
                new HistoricoAportesResponseDTO(LocalDate.of(2025, 1, 25), new BigDecimal("1000.00"), "3/3")
        );

        when(valoresService.consultarHistoricoAportes(clienteId)).thenReturn(historicos);

        // Act & Assert
        mockMvc.perform(get("/api/valores/historico/{clienteId}", clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].data[0]").value(2025))
                .andExpect(jsonPath("$[0].data[1]").value(1))
                .andExpect(jsonPath("$[0].data[2]").value(5))
                .andExpect(jsonPath("$[0].parcela").value("1/3"))
                .andExpect(jsonPath("$[1].data[2]").value(15))
                .andExpect(jsonPath("$[1].parcela").value("2/3"))
                .andExpect(jsonPath("$[2].data[2]").value(25))
                .andExpect(jsonPath("$[2].parcela").value("3/3"));
    }
}

