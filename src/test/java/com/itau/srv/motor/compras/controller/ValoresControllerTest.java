package com.itau.srv.motor.compras.controller;

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
}

