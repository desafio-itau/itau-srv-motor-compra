package com.itau.srv.motor.compras.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itau.srv.motor.compras.dto.ir.RebalancementoEventDTO;
import com.itau.srv.motor.compras.dto.rebalanceamento.*;
import com.itau.srv.motor.compras.kafka.RebalanceamentoEventProducer;
import com.itau.srv.motor.compras.service.RebalanceamentoService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RebalanceamentoController - Testes Unitários")
class RebalanceamentoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RebalanceamentoEventProducer rebalanceamentoEventProducer;

    @Mock
    private RebalanceamentoService rebalanceamentoService;

    @InjectMocks
    private RebalanceamentoController rebalanceamentoController;

    private ObjectMapper objectMapper;
    private RebalanceamentoDesvioResponseDTO responseDesvioDTO;

    @BeforeEach
    void setUp() {
        // Configurar MockMvc standalone (sem Spring context)
        mockMvc = MockMvcBuilders.standaloneSetup(rebalanceamentoController).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Mock padrão de resposta
        responseDesvioDTO = criarResponseDesvioMock();
    }

    // =====================================================
    // TESTES DO ENDPOINT /eventos (Rebalanceamento por Mudança de Cesta)
    // =====================================================

    @Test
    @DisplayName("Deve publicar evento de rebalanceamento com sucesso")
    void devePublicarEventoRebalanceamentoComSucesso() throws Exception {
        // Arrange
        RebalancementoEventDTO evento = new RebalancementoEventDTO(
                1L,
                2L,
                LocalDateTime.of(2026, 3, 3, 10, 0)
        );

        String json = objectMapper.writeValueAsString(evento);

        doNothing().when(rebalanceamentoEventProducer).publicarEventoRebalanceamento(any());

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());

        verify(rebalanceamentoEventProducer, times(1))
                .publicarEventoRebalanceamento(any(RebalancementoEventDTO.class));
    }

    @Test
    @DisplayName("Deve retornar 400 quando body ausente no endpoint /eventos")
    void deveRetornar400QuandoBodyAusenteNoEndpointEventos() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/eventos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(rebalanceamentoEventProducer, never()).publicarEventoRebalanceamento(any());
    }

    @Test
    @DisplayName("Deve aceitar apenas método POST no endpoint /eventos")
    void deveAceitarApenasMetodoPostNoEndpointEventos() throws Exception {
        // Act & Assert - GET não deve funcionar
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/rebalanceamentos/eventos"))
                .andExpect(status().isMethodNotAllowed());

        verify(rebalanceamentoEventProducer, never()).publicarEventoRebalanceamento(any());
    }

    @Test
    @DisplayName("Deve retornar 415 quando Content-Type está ausente no endpoint /eventos")
    void deveRetornar415QuandoContentTypeAusenteNoEndpointEventos() throws Exception {
        // Arrange
        RebalancementoEventDTO evento = new RebalancementoEventDTO(
                1L,
                2L,
                LocalDateTime.of(2026, 3, 3, 10, 0)
        );

        String json = objectMapper.writeValueAsString(evento);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/eventos")
                        .content(json))
                .andExpect(status().isUnsupportedMediaType());

        verify(rebalanceamentoEventProducer, never()).publicarEventoRebalanceamento(any());
    }

    // =====================================================
    // TESTES DO ENDPOINT /desvio (Rebalanceamento por Desvio)
    // =====================================================

    @Test
    @DisplayName("Deve executar rebalanceamento por desvio com sucesso")
    void deveExecutarRebalanceamentoPorDesvioComSucesso() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                any(BigDecimal.class),
                any(LocalDate.class)
        )).thenReturn(responseDesvioDTO);

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientesProcessados").value(1))
                .andExpect(jsonPath("$.totalClientesRebalanceados").value(1))
                .andExpect(jsonPath("$.totalOperacoes").value(4))
                .andExpect(jsonPath("$.mensagem").value("Rebalanceamento por desvio executado com sucesso. 1/1 clientes rebalanceados."));

        verify(rebalanceamentoService, times(1))
                .executarRebalanceamentoPorDesvio(any(BigDecimal.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve retornar 200 OK com response válido no endpoint /desvio")
    void deveRetornar200OkComResponseValidoNoEndpointDesvio() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                any(BigDecimal.class),
                any(LocalDate.class)
        )).thenReturn(responseDesvioDTO);

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalClientesProcessados").exists())
                .andExpect(jsonPath("$.totalClientesRebalanceados").exists())
                .andExpect(jsonPath("$.totalOperacoes").exists())
                .andExpect(jsonPath("$.clientesRebalanceados").isArray())
                .andExpect(jsonPath("$.mensagem").exists());

        verify(rebalanceamentoService, times(1))
                .executarRebalanceamentoPorDesvio(any(BigDecimal.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve usar limiar padrão 5.0 quando fornecido")
    void deveUsarLimiarPadraoQuandoFornecido() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                eq(new BigDecimal("5.0")),
                eq(LocalDate.of(2026, 3, 4))
        )).thenReturn(responseDesvioDTO);

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(rebalanceamentoService).executarRebalanceamentoPorDesvio(
                eq(new BigDecimal("5.0")),
                eq(LocalDate.of(2026, 3, 4))
        );
    }

    @Test
    @DisplayName("Deve aceitar limiar personalizado")
    void deveAceitarLimiarPersonalizado() throws Exception {
        // Arrange
        BigDecimal limiarCustomizado = new BigDecimal("10.0");
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                limiarCustomizado,
                LocalDate.of(2026, 3, 4)
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                eq(limiarCustomizado),
                any(LocalDate.class)
        )).thenReturn(responseDesvioDTO);

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(rebalanceamentoService).executarRebalanceamentoPorDesvio(
                eq(limiarCustomizado),
                eq(LocalDate.of(2026, 3, 4))
        );
    }

    @Test
    @DisplayName("Deve retornar 400 quando body ausente no endpoint /desvio")
    void deveRetornar400QuandoBodyAusenteNoEndpointDesvio() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(rebalanceamentoService, never())
                .executarRebalanceamentoPorDesvio(any(BigDecimal.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve aceitar apenas método POST no endpoint /desvio")
    void deveAceitarApenasMetodoPostNoEndpointDesvio() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert - GET não deve funcionar
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isMethodNotAllowed());

        // Act & Assert - PUT não deve funcionar
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isMethodNotAllowed());

        // Act & Assert - DELETE não deve funcionar
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isMethodNotAllowed());

        verify(rebalanceamentoService, never())
                .executarRebalanceamentoPorDesvio(any(BigDecimal.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve retornar 415 quando Content-Type está ausente no endpoint /desvio")
    void deveRetornar415QuandoContentTypeAusenteNoEndpointDesvio() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .content(json))
                .andExpect(status().isUnsupportedMediaType());

        verify(rebalanceamentoService, never())
                .executarRebalanceamentoPorDesvio(any(BigDecimal.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve validar caminho da URL /desvio")
    void deveValidarCaminhoDaURLDesvio() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                any(BigDecimal.class),
                any(LocalDate.class)
        )).thenReturn(responseDesvioDTO);

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert - URL correta
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // URL incorreta deve retornar 404
        mockMvc.perform(post("/api/rebalanceamentos/desvios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar resposta vazia quando nenhum cliente rebalanceado")
    void deveRetornarRespostaVaziaQuandoNenhumClienteRebalanceado() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        RebalanceamentoDesvioResponseDTO responseVazio = new RebalanceamentoDesvioResponseDTO(
                5,
                0,
                0,
                Collections.emptyList(),
                "Rebalanceamento por desvio executado com sucesso. 0/5 clientes rebalanceados."
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                any(BigDecimal.class),
                any(LocalDate.class)
        )).thenReturn(responseVazio);

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientesProcessados").value(5))
                .andExpect(jsonPath("$.totalClientesRebalanceados").value(0))
                .andExpect(jsonPath("$.clientesRebalanceados").isEmpty());

        verify(rebalanceamentoService, times(1))
                .executarRebalanceamentoPorDesvio(any(BigDecimal.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve chamar service com parâmetros corretos")
    void deveChamarServiceComParametrosCorretos() throws Exception {
        // Arrange
        BigDecimal limiarEsperado = new BigDecimal("7.5");
        LocalDate dataEsperada = LocalDate.of(2026, 3, 10);

        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                limiarEsperado,
                dataEsperada
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                eq(limiarEsperado),
                eq(dataEsperada)
        )).thenReturn(responseDesvioDTO);

        String json = objectMapper.writeValueAsString(request);

        // Act
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // Assert
        verify(rebalanceamentoService).executarRebalanceamentoPorDesvio(
                eq(limiarEsperado),
                eq(dataEsperada)
        );
    }

    @Test
    @DisplayName("Deve serializar LocalDate corretamente no request")
    void deveSerializarLocalDateCorretamenteNoRequest() throws Exception {
        // Arrange
        LocalDate dataEsperada = LocalDate.of(2026, 3, 15);
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                dataEsperada
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                any(BigDecimal.class),
                eq(dataEsperada)
        )).thenReturn(responseDesvioDTO);

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(rebalanceamentoService).executarRebalanceamentoPorDesvio(
                any(BigDecimal.class),
                eq(dataEsperada)
        );
    }

    @Test
    @DisplayName("Deve processar múltiplas requisições sequencialmente")
    void deveProcessarMultiplasRequisicoesSequencialmente() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request1 = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        RebalanceamentoDesvioRequestDTO request2 = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("10.0"),
                LocalDate.of(2026, 3, 5)
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                any(BigDecimal.class),
                any(LocalDate.class)
        )).thenReturn(responseDesvioDTO);

        String json1 = objectMapper.writeValueAsString(request1);
        String json2 = objectMapper.writeValueAsString(request2);

        // Act & Assert - Primeira requisição
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json1))
                .andExpect(status().isOk());

        // Act & Assert - Segunda requisição
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json2))
                .andExpect(status().isOk());

        verify(rebalanceamentoService, times(2))
                .executarRebalanceamentoPorDesvio(any(BigDecimal.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve retornar response com todos os campos necessários")
    void deveRetornarResponseComTodosOsCamposNecessarios() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                any(BigDecimal.class),
                any(LocalDate.class)
        )).thenReturn(responseDesvioDTO);

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientesProcessados").isNumber())
                .andExpect(jsonPath("$.totalClientesRebalanceados").isNumber())
                .andExpect(jsonPath("$.totalOperacoes").isNumber())
                .andExpect(jsonPath("$.clientesRebalanceados").isArray())
                .andExpect(jsonPath("$.mensagem").isString());

        verify(rebalanceamentoService, times(1))
                .executarRebalanceamentoPorDesvio(any(BigDecimal.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve validar estrutura de clientesRebalanceados no response")
    void deveValidarEstruturaDeClientesRebalanceadosNoResponse() throws Exception {
        // Arrange
        RebalanceamentoDesvioRequestDTO request = new RebalanceamentoDesvioRequestDTO(
                new BigDecimal("5.0"),
                LocalDate.of(2026, 3, 4)
        );

        when(rebalanceamentoService.executarRebalanceamentoPorDesvio(
                any(BigDecimal.class),
                any(LocalDate.class)
        )).thenReturn(responseDesvioDTO);

        String json = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/rebalanceamentos/desvio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientesRebalanceados[0].clienteId").exists())
                .andExpect(jsonPath("$.clientesRebalanceados[0].nome").exists())
                .andExpect(jsonPath("$.clientesRebalanceados[0].operacoes").isArray())
                .andExpect(jsonPath("$.clientesRebalanceados[0].operacoes[0].ticker").exists())
                .andExpect(jsonPath("$.clientesRebalanceados[0].operacoes[0].tipoOperacao").exists())
                .andExpect(jsonPath("$.clientesRebalanceados[0].operacoes[0].quantidade").exists())
                .andExpect(jsonPath("$.clientesRebalanceados[0].operacoes[0].precoUnitario").exists())
                .andExpect(jsonPath("$.clientesRebalanceados[0].operacoes[0].valorTotal").exists());

        verify(rebalanceamentoService, times(1))
                .executarRebalanceamentoPorDesvio(any(BigDecimal.class), any(LocalDate.class));
    }

    // Helper methods

    private RebalanceamentoDesvioResponseDTO criarResponseDesvioMock() {
        List<OperacaoRebalanceamentoDTO> operacoes = List.of(
                new OperacaoRebalanceamentoDTO(
                        "LREN3",
                        "VENDA",
                        151,
                        new BigDecimal("13.39"),
                        new BigDecimal("2021.89"),
                        new BigDecimal("60.19"),
                        new BigDecimal("40.00"),
                        new BigDecimal("20.19")
                ),
                new OperacaoRebalanceamentoDTO(
                        "MOVI3",
                        "COMPRA",
                        150,
                        new BigDecimal("9.93"),
                        new BigDecimal("1489.50"),
                        new BigDecimal("4.98"),
                        new BigDecimal("20.00"),
                        new BigDecimal("15.02")
                ),
                new OperacaoRebalanceamentoDTO(
                        "BMEB3",
                        "VENDA",
                        13,
                        new BigDecimal("63.59"),
                        new BigDecimal("826.67"),
                        new BigDecimal("17.87"),
                        new BigDecimal("10.00"),
                        new BigDecimal("7.87")
                ),
                new OperacaoRebalanceamentoDTO(
                        "BMGB4",
                        "COMPRA",
                        261,
                        new BigDecimal("4.97"),
                        new BigDecimal("1297.17"),
                        new BigDecimal("6.98"),
                        new BigDecimal("20.00"),
                        new BigDecimal("13.02")
                )
        );

        ClienteRebalanceadoDTO cliente = new ClienteRebalanceadoDTO(
                11L,
                "Cliente Teste Desvio",
                operacoes
        );

        return new RebalanceamentoDesvioResponseDTO(
                1,
                1,
                4,
                List.of(cliente),
                "Rebalanceamento por desvio executado com sucesso. 1/1 clientes rebalanceados."
        );
    }
}



