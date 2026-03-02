package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.custodia.CustodiaResponseDTO;
import com.itau.srv.motor.compras.service.CustodiaService;
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
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustodiaController - Testes Unitários")
class CustodiaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustodiaService custodiaService;

    @InjectMocks
    private CustodiaController custodiaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(custodiaController).build();
    }

    @Test
    @DisplayName("Deve buscar custodias de cliente com sucesso")
    void deveBuscarCustodiasDeClienteComSucesso() throws Exception {
        // Arrange
        Long clienteId = 1L;

        CustodiaResponseDTO custodia1 = new CustodiaResponseDTO(
                "PETR4",
                100,
                new BigDecimal("35.50"),
                new BigDecimal("36.00"),
                "Resíduo distribuição 2026-03-01"
        );

        CustodiaResponseDTO custodia2 = new CustodiaResponseDTO(
                "VALE3",
                50,
                new BigDecimal("62.00"),
                new BigDecimal("63.50"),
                "Resíduo distribuição 2026-03-01"
        );

        List<CustodiaResponseDTO> custodias = List.of(custodia1, custodia2);

        when(custodiaService.consultarCustodiasCliente(clienteId)).thenReturn(custodias);

        // Act & Assert
        mockMvc.perform(get("/api/custodias/{clienteId}", clienteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].quantidade").value(100))
                .andExpect(jsonPath("$[0].precoMedio").value(35.50))
                .andExpect(jsonPath("$[0].valorAtual").value(36.00))
                .andExpect(jsonPath("$[0].origem").value("Resíduo distribuição 2026-03-01"))
                .andExpect(jsonPath("$[1].ticker").value("VALE3"))
                .andExpect(jsonPath("$[1].quantidade").value(50))
                .andExpect(jsonPath("$[1].precoMedio").value(62.00))
                .andExpect(jsonPath("$[1].valorAtual").value(63.50));

        verify(custodiaService).consultarCustodiasCliente(clienteId);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando cliente não possui custodias")
    void deveRetornarListaVaziaQuandoClienteNaoPossuiCustodias() throws Exception {
        // Arrange
        Long clienteId = 1L;

        when(custodiaService.consultarCustodiasCliente(clienteId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/custodias/{clienteId}", clienteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(custodiaService).consultarCustodiasCliente(clienteId);
    }

    @Test
    @DisplayName("Deve buscar custodias com um único ativo")
    void deveBuscarCustodiasComUmUnicoAtivo() throws Exception {
        // Arrange
        Long clienteId = 1L;

        CustodiaResponseDTO custodia = new CustodiaResponseDTO(
                "PETR4",
                25,
                new BigDecimal("35.50"),
                new BigDecimal("36.00"),
                "Resíduo distribuição 2026-03-01"
        );

        when(custodiaService.consultarCustodiasCliente(clienteId)).thenReturn(List.of(custodia));

        // Act & Assert
        mockMvc.perform(get("/api/custodias/{clienteId}", clienteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].quantidade").value(25));

        verify(custodiaService).consultarCustodiasCliente(clienteId);
    }

    @Test
    @DisplayName("Deve buscar custodias com todos os 5 ativos")
    void deveBuscarCustodiasComTodosOs5Ativos() throws Exception {
        // Arrange
        Long clienteId = 1L;

        List<CustodiaResponseDTO> custodias = List.of(
                new CustodiaResponseDTO("PETR4", 100, new BigDecimal("35.50"), new BigDecimal("36.00"), "Resíduo distribuição 2026-03-01"),
                new CustodiaResponseDTO("VALE3", 50, new BigDecimal("62.00"), new BigDecimal("63.50"), "Resíduo distribuição 2026-03-01"),
                new CustodiaResponseDTO("BBDC4", 200, new BigDecimal("25.00"), new BigDecimal("25.50"), "Resíduo distribuição 2026-03-01"),
                new CustodiaResponseDTO("ITUB4", 150, new BigDecimal("30.00"), new BigDecimal("30.75"), "Resíduo distribuição 2026-03-01"),
                new CustodiaResponseDTO("ABEV3", 300, new BigDecimal("12.50"), new BigDecimal("12.75"), "Resíduo distribuição 2026-03-01")
        );

        when(custodiaService.consultarCustodiasCliente(clienteId)).thenReturn(custodias);

        // Act & Assert
        mockMvc.perform(get("/api/custodias/{clienteId}", clienteId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[1].ticker").value("VALE3"))
                .andExpect(jsonPath("$[2].ticker").value("BBDC4"))
                .andExpect(jsonPath("$[3].ticker").value("ITUB4"))
                .andExpect(jsonPath("$[4].ticker").value("ABEV3"));

        verify(custodiaService).consultarCustodiasCliente(clienteId);
    }

    @Test
    @DisplayName("Deve aceitar diferentes IDs de cliente")
    void deveAceitarDiferentesIdsDeCliente() throws Exception {
        // Arrange
        Long clienteId1 = 1L;
        Long clienteId2 = 999L;

        CustodiaResponseDTO custodia1 = new CustodiaResponseDTO(
                "PETR4",
                100,
                new BigDecimal("35.50"),
                new BigDecimal("36.00"),
                "Resíduo distribuição 2026-03-01"
        );

        CustodiaResponseDTO custodia2 = new CustodiaResponseDTO(
                "VALE3",
                200,
                new BigDecimal("62.00"),
                new BigDecimal("63.50"),
                "Resíduo distribuição 2026-03-01"
        );

        when(custodiaService.consultarCustodiasCliente(clienteId1)).thenReturn(List.of(custodia1));
        when(custodiaService.consultarCustodiasCliente(clienteId2)).thenReturn(List.of(custodia2));

        // Act & Assert - Cliente 1
        mockMvc.perform(get("/api/custodias/{clienteId}", clienteId1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$[0].quantidade").value(100));

        // Act & Assert - Cliente 2
        mockMvc.perform(get("/api/custodias/{clienteId}", clienteId2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticker").value("VALE3"))
                .andExpect(jsonPath("$[0].quantidade").value(200));

        verify(custodiaService).consultarCustodiasCliente(clienteId1);
        verify(custodiaService).consultarCustodiasCliente(clienteId2);
    }

    @Test
    @DisplayName("Deve retornar status 200 OK")
    void deveRetornarStatus200OK() throws Exception {
        // Arrange
        Long clienteId = 1L;

        when(custodiaService.consultarCustodiasCliente(clienteId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/custodias/{clienteId}", clienteId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve validar caminho da URL")
    void deveValidarCaminhoDaURL() throws Exception {
        // Arrange
        Long clienteId = 123L;

        CustodiaResponseDTO custodia = new CustodiaResponseDTO(
                "PETR4",
                100,
                new BigDecimal("35.50"),
                new BigDecimal("36.00"),
                "Resíduo distribuição 2026-03-01"
        );

        when(custodiaService.consultarCustodiasCliente(clienteId)).thenReturn(List.of(custodia));

        // Act & Assert
        mockMvc.perform(get("/api/custodias/123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("PETR4"));

        verify(custodiaService).consultarCustodiasCliente(123L);
    }

    @Test
    @DisplayName("Deve retornar Content-Type JSON")
    void deveRetornarContentTypeJSON() throws Exception {
        // Arrange
        Long clienteId = 1L;

        when(custodiaService.consultarCustodiasCliente(clienteId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/custodias/{clienteId}", clienteId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Deve chamar service com o clienteId correto")
    void deveChamarServiceComClienteIdCorreto() throws Exception {
        // Arrange
        Long clienteId = 42L;

        when(custodiaService.consultarCustodiasCliente(clienteId)).thenReturn(List.of());

        // Act
        mockMvc.perform(get("/api/custodias/{clienteId}", clienteId))
                .andExpect(status().isOk());

        // Assert
        verify(custodiaService).consultarCustodiasCliente(eq(42L));
        verify(custodiaService, times(1)).consultarCustodiasCliente(any());
    }
}

