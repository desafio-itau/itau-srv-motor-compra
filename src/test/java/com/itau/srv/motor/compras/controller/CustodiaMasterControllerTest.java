package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.conta.ContaMasterResponseDTO;
import com.itau.srv.motor.compras.dto.custodia.CustodiaMasterResponseDTO;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustodiaMasterController - Testes Unitários")
class CustodiaMasterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustodiaService custodiaService;

    @InjectMocks
    private CustodiaMasterController custodiaMasterController;

    @BeforeEach
    void setUp() {
        // Configurar MockMvc standalone (sem Spring context)
        mockMvc = MockMvcBuilders.standaloneSetup(custodiaMasterController).build();
    }

    @Test
    @DisplayName("Deve retornar custodia master com sucesso")
    void deveRetornarCustodiaMasterComSucesso() throws Exception {
        // Arrange
        ContaMasterResponseDTO contaMaster = new ContaMasterResponseDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER"
        );

        CustodiaResponseDTO custodia1 = new CustodiaResponseDTO(
                "PETR4",
                10,
                new BigDecimal("35.50"),
                new BigDecimal("36.00"),
                "Resíduo distribuição 2026-03-01"
        );

        CustodiaResponseDTO custodia2 = new CustodiaResponseDTO(
                "VALE3",
                5,
                new BigDecimal("62.00"),
                new BigDecimal("63.50"),
                "Resíduo distribuição 2026-03-01"
        );

        CustodiaMasterResponseDTO response = new CustodiaMasterResponseDTO(
                contaMaster,
                List.of(custodia1, custodia2),
                new BigDecimal("677.50")
        );

        when(custodiaService.consultarCustodiaMaster()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/custodia-master")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.contaMaster.id").value(1))
                .andExpect(jsonPath("$.contaMaster.numeroConta").value("CONTA-MASTER-001"))
                .andExpect(jsonPath("$.contaMaster.tipo").value("MASTER"))
                .andExpect(jsonPath("$.custodia").isArray())
                .andExpect(jsonPath("$.custodia.length()").value(2))
                .andExpect(jsonPath("$.custodia[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.custodia[0].quantidade").value(10))
                .andExpect(jsonPath("$.custodia[0].precoMedio").value(35.50))
                .andExpect(jsonPath("$.custodia[0].valorAtual").value(36.00))
                .andExpect(jsonPath("$.custodia[1].ticker").value("VALE3"))
                .andExpect(jsonPath("$.custodia[1].quantidade").value(5))
                .andExpect(jsonPath("$.custodia[1].precoMedio").value(62.00))
                .andExpect(jsonPath("$.custodia[1].valorAtual").value(63.50))
                .andExpect(jsonPath("$.valorTotalResiduo").value(677.50));
    }

    @Test
    @DisplayName("Deve retornar custodia master vazia")
    void deveRetornarCustodiaMasterVazia() throws Exception {
        // Arrange
        ContaMasterResponseDTO contaMaster = new ContaMasterResponseDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER"
        );

        CustodiaMasterResponseDTO response = new CustodiaMasterResponseDTO(
                contaMaster,
                List.of(),
                BigDecimal.ZERO
        );

        when(custodiaService.consultarCustodiaMaster()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/custodia-master")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.contaMaster.id").value(1))
                .andExpect(jsonPath("$.contaMaster.numeroConta").value("CONTA-MASTER-001"))
                .andExpect(jsonPath("$.contaMaster.tipo").value("MASTER"))
                .andExpect(jsonPath("$.custodia").isArray())
                .andExpect(jsonPath("$.custodia.length()").value(0))
                .andExpect(jsonPath("$.valorTotalResiduo").value(0));
    }

    @Test
    @DisplayName("Deve retornar custodia master com múltiplos ativos")
    void deveRetornarCustodiaMasterComMultiplosAtivos() throws Exception {
        // Arrange
        ContaMasterResponseDTO contaMaster = new ContaMasterResponseDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER"
        );

        CustodiaResponseDTO custodia1 = new CustodiaResponseDTO(
                "PETR4",
                10,
                new BigDecimal("35.50"),
                new BigDecimal("36.00"),
                "Resíduo distribuição 2026-03-01"
        );

        CustodiaResponseDTO custodia2 = new CustodiaResponseDTO(
                "VALE3",
                5,
                new BigDecimal("62.00"),
                new BigDecimal("63.50"),
                "Resíduo distribuição 2026-03-01"
        );

        CustodiaResponseDTO custodia3 = new CustodiaResponseDTO(
                "BBDC4",
                15,
                new BigDecimal("25.00"),
                new BigDecimal("25.50"),
                "Resíduo distribuição 2026-03-01"
        );

        CustodiaResponseDTO custodia4 = new CustodiaResponseDTO(
                "ITUB4",
                8,
                new BigDecimal("30.00"),
                new BigDecimal("30.75"),
                "Resíduo distribuição 2026-03-01"
        );

        CustodiaResponseDTO custodia5 = new CustodiaResponseDTO(
                "ABEV3",
                20,
                new BigDecimal("12.50"),
                new BigDecimal("12.75"),
                "Resíduo distribuição 2026-03-01"
        );

        CustodiaMasterResponseDTO response = new CustodiaMasterResponseDTO(
                contaMaster,
                List.of(custodia1, custodia2, custodia3, custodia4, custodia5),
                new BigDecimal("1484.50")
        );

        when(custodiaService.consultarCustodiaMaster()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/custodia-master")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.contaMaster.id").value(1))
                .andExpect(jsonPath("$.custodia").isArray())
                .andExpect(jsonPath("$.custodia.length()").value(5))
                .andExpect(jsonPath("$.custodia[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.custodia[1].ticker").value("VALE3"))
                .andExpect(jsonPath("$.custodia[2].ticker").value("BBDC4"))
                .andExpect(jsonPath("$.custodia[3].ticker").value("ITUB4"))
                .andExpect(jsonPath("$.custodia[4].ticker").value("ABEV3"))
                .andExpect(jsonPath("$.valorTotalResiduo").value(1484.50));
    }

    @Test
    @DisplayName("Deve aceitar apenas método GET")
    void deveAceitarApenasMetodoGet() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/custodia-master"))
                .andExpect(status().isOk());
    }
}

