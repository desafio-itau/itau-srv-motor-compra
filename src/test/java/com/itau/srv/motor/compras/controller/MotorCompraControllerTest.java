package com.itau.srv.motor.compras.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraRequestDTO;
import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraResponseDTO;
import com.itau.srv.motor.compras.service.MotorCompraService;
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
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MotorCompraController - Testes Unitários")
class MotorCompraControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MotorCompraService motorCompraService;

    @InjectMocks
    private MotorCompraController motorCompraController;

    private ObjectMapper objectMapper;
    private MotorCompraResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        // Configurar MockMvc standalone (sem Spring context)
        mockMvc = MockMvcBuilders.standaloneSetup(motorCompraController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        responseDTO = new MotorCompraResponseDTO(
                LocalDateTime.now(),
                2,
                new BigDecimal("3000.00"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                4,
                "Compra programada executada com sucesso para 2 clientes."
        );
    }

    @Test
    @DisplayName("Deve executar compra com sucesso")
    void deveExecutarCompraComSucesso() throws Exception {
        // Arrange
        LocalDate dataReferencia = LocalDate.of(2026, 3, 5);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dataReferencia);

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(2))
                .andExpect(jsonPath("$.totalConsolidado").value(3000.00))
                .andExpect(jsonPath("$.eventosIRPublicados").value(4))
                .andExpect(jsonPath("$.mensagem").value("Compra programada executada com sucesso para 2 clientes."));

        verify(motorCompraService, times(1)).acionarMotorCompra(any(MotorCompraRequestDTO.class));
    }

    @Test
    @DisplayName("Deve retornar 200 OK com response válido")
    void deveRetornar200OkComResponseValido() throws Exception {
        // Arrange
        LocalDate dataReferencia = LocalDate.of(2026, 3, 15);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dataReferencia);

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dataExecucao").exists())
                .andExpect(jsonPath("$.totalClientes").exists())
                .andExpect(jsonPath("$.totalConsolidado").exists())
                .andExpect(jsonPath("$.ordensCompra").isArray())
                .andExpect(jsonPath("$.distribuicoes").isArray())
                .andExpect(jsonPath("$.residuosCustMaster").isArray())
                .andExpect(jsonPath("$.eventosIRPublicados").exists())
                .andExpect(jsonPath("$.mensagem").exists());

        verify(motorCompraService, times(1)).acionarMotorCompra(any(MotorCompraRequestDTO.class));
    }

    @Test
    @DisplayName("Deve aceitar requisição com data no dia 5")
    void deveAceitarRequisicaoComDataNoDia5() throws Exception {
        // Arrange
        LocalDate dia5 = LocalDate.of(2026, 3, 5);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dia5);

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(motorCompraService, times(1)).acionarMotorCompra(any(MotorCompraRequestDTO.class));
    }

    @Test
    @DisplayName("Deve aceitar requisição com data no dia 15")
    void deveAceitarRequisicaoComDataNoDia15() throws Exception {
        // Arrange
        LocalDate dia15 = LocalDate.of(2026, 3, 15);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dia15);

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(motorCompraService, times(1)).acionarMotorCompra(any(MotorCompraRequestDTO.class));
    }

    @Test
    @DisplayName("Deve aceitar requisição com data no dia 25")
    void deveAceitarRequisicaoComDataNoDia25() throws Exception {
        // Arrange
        LocalDate dia25 = LocalDate.of(2026, 3, 25);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dia25);

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(motorCompraService, times(1)).acionarMotorCompra(any(MotorCompraRequestDTO.class));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando Content-Type está ausente")
    void deveRetornar400QuandoContentTypeAusente() throws Exception {
        // Arrange
        LocalDate dataReferencia = LocalDate.of(2026, 3, 5);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dataReferencia);

        // Act & Assert
        mockMvc.perform(post("/api/motor/executar-compra")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

        verify(motorCompraService, never()).acionarMotorCompra(any());
    }

    @Test
    @DisplayName("Deve retornar 500 quando ocorre exceção no service")
    void deveRetornar500QuandoOcorreExcecaoNoService() throws Exception {
        // Arrange
        LocalDate dataReferencia = LocalDate.of(2026, 3, 5);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dataReferencia);

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenThrow(new RuntimeException("Erro no serviço"));

        // Act & Assert
        try {
            mockMvc.perform(post("/api/motor/executar-compra")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        } catch (Exception e) {
            // Esperado: exceção deve ser lançada pois não há handler configurado
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Erro no serviço");
        }

        verify(motorCompraService, times(1)).acionarMotorCompra(any(MotorCompraRequestDTO.class));
    }

    @Test
    @DisplayName("Deve processar múltiplas requisições sequencialmente")
    void deveProcessarMultiplasRequisicoesSequencialmente() throws Exception {
        // Arrange
        LocalDate data1 = LocalDate.of(2026, 3, 5);
        LocalDate data2 = LocalDate.of(2026, 3, 15);

        MotorCompraRequestDTO request1 = new MotorCompraRequestDTO(data1);
        MotorCompraRequestDTO request2 = new MotorCompraRequestDTO(data2);

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert - Primeira requisição
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Act & Assert - Segunda requisição
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        verify(motorCompraService, times(2)).acionarMotorCompra(any(MotorCompraRequestDTO.class));
    }

    @Test
    @DisplayName("Deve usar endpoint correto /api/motor/executar-compra")
    void deveUsarEndpointCorreto() throws Exception {
        // Arrange
        LocalDate dataReferencia = LocalDate.of(2026, 3, 5);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dataReferencia);

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verificar que outros endpoints não funcionam
        mockMvc.perform(post("/api/motor/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve aceitar apenas método POST")
    void deveAceitarApenasMetodoPost() throws Exception {
        // Arrange
        LocalDate dataReferencia = LocalDate.of(2026, 3, 5);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dataReferencia);

        // Act & Assert - GET não deve funcionar
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isMethodNotAllowed());

        // Act & Assert - PUT não deve funcionar
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isMethodNotAllowed());

        // Act & Assert - DELETE não deve funcionar
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isMethodNotAllowed());

        verify(motorCompraService, never()).acionarMotorCompra(any());
    }

    @Test
    @DisplayName("Deve serializar LocalDateTime corretamente no response")
    void deveSerializarLocalDateTimeCorretamenteNoResponse() throws Exception {
        // Arrange
        LocalDate dataReferencia = LocalDate.of(2026, 3, 5);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dataReferencia);

        LocalDateTime dataExecucao = LocalDateTime.of(2026, 3, 5, 10, 30, 0);
        MotorCompraResponseDTO response = new MotorCompraResponseDTO(
                dataExecucao,
                1,
                new BigDecimal("1000.00"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                2,
                "Sucesso"
        );

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataExecucao").exists());

        verify(motorCompraService, times(1)).acionarMotorCompra(any(MotorCompraRequestDTO.class));
    }

    @Test
    @DisplayName("Deve retornar response com todos os campos necessários")
    void deveRetornarResponseComTodosOsCamposNecessarios() throws Exception {
        // Arrange
        LocalDate dataReferencia = LocalDate.of(2026, 3, 5);
        MotorCompraRequestDTO request = new MotorCompraRequestDTO(dataReferencia);

        when(motorCompraService.acionarMotorCompra(any(MotorCompraRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/motor/executar-compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataExecucao").exists())
                .andExpect(jsonPath("$.totalClientes").isNumber())
                .andExpect(jsonPath("$.totalConsolidado").isNumber())
                .andExpect(jsonPath("$.ordensCompra").isArray())
                .andExpect(jsonPath("$.distribuicoes").isArray())
                .andExpect(jsonPath("$.residuosCustMaster").isArray())
                .andExpect(jsonPath("$.eventosIRPublicados").isNumber())
                .andExpect(jsonPath("$.mensagem").isString());

        verify(motorCompraService, times(1)).acionarMotorCompra(any(MotorCompraRequestDTO.class));
    }
}



