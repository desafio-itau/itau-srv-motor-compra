package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.agrupamento.AgrupamentoResponseDTO;
import com.itau.srv.motor.compras.dto.agrupamento.ClienteAgrupamentoDTO;
import com.itau.srv.motor.compras.dto.cliente.ClienteResponseDTO;
import com.itau.srv.motor.compras.dto.conta.ContaGraficaDTO;
import com.itau.srv.motor.compras.feign.ClientesFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - Testes Unitários")
class ClienteServiceTest {

    @Mock
    private ClientesFeignClient clientesFeignClient;

    @InjectMocks
    private ClienteService clienteService;

    private ClienteResponseDTO cliente1;
    private ClienteResponseDTO cliente2;
    private ClienteResponseDTO cliente3;

    @BeforeEach
    void setUp() {
        cliente1 = new ClienteResponseDTO(
                1L,
                "João Silva",
                "12345678901",
                "joao@email.com",
                new BigDecimal("3000.00"),
                true,
                LocalDateTime.now(),
                new ContaGraficaDTO(1L, "FLH-001", "FILHOTE", LocalDateTime.now())
        );

        cliente2 = new ClienteResponseDTO(
                2L,
                "Maria Santos",
                "98765432100",
                "maria@email.com",
                new BigDecimal("6000.00"),
                true,
                LocalDateTime.now(),
                new ContaGraficaDTO(2L, "FLH-002", "FILHOTE", LocalDateTime.now())
        );

        cliente3 = new ClienteResponseDTO(
                3L,
                "Pedro Costa",
                "11122233344",
                "pedro@email.com",
                new BigDecimal("1500.00"),
                true,
                LocalDateTime.now(),
                new ContaGraficaDTO(3L, "FLH-003", "FILHOTE", LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("Deve agrupar clientes ativos com sucesso")
    void deveAgruparClientesAtivosComSucesso() {
        // Arrange
        List<ClienteResponseDTO> clientesAtivos = Arrays.asList(cliente1, cliente2, cliente3);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(clientesAtivos);

        // Act
        AgrupamentoResponseDTO resultado = clienteService.agruparClientes();

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.agrupamentoCliente()).hasSize(3);
        assertThat(resultado.valorConsolidado()).isEqualByComparingTo(new BigDecimal("3500.00")); // (3000 + 6000 + 1500) / 3

        // Verificar cliente 1
        ClienteAgrupamentoDTO clienteAgrupado1 = resultado.agrupamentoCliente().get(0);
        assertThat(clienteAgrupado1.clienteId()).isEqualTo(1L);
        assertThat(clienteAgrupado1.nome()).isEqualTo("João Silva");
        assertThat(clienteAgrupado1.cpf()).isEqualTo("12345678901");
        assertThat(clienteAgrupado1.contaId()).isEqualTo(1L);
        assertThat(clienteAgrupado1.valorAportado()).isEqualByComparingTo(new BigDecimal("1000.00")); // 3000 / 3

        // Verificar cliente 2
        ClienteAgrupamentoDTO clienteAgrupado2 = resultado.agrupamentoCliente().get(1);
        assertThat(clienteAgrupado2.valorAportado()).isEqualByComparingTo(new BigDecimal("2000.00")); // 6000 / 3

        // Verificar cliente 3
        ClienteAgrupamentoDTO clienteAgrupado3 = resultado.agrupamentoCliente().get(2);
        assertThat(clienteAgrupado3.valorAportado()).isEqualByComparingTo(new BigDecimal("500.00")); // 1500 / 3

        verify(clientesFeignClient, times(1)).listarClientesAtivos();
    }

    @Test
    @DisplayName("Deve agrupar um único cliente")
    void deveAgruparUmUnicoCliente() {
        // Arrange
        List<ClienteResponseDTO> clientesAtivos = Collections.singletonList(cliente1);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(clientesAtivos);

        // Act
        AgrupamentoResponseDTO resultado = clienteService.agruparClientes();

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.agrupamentoCliente()).hasSize(1);
        assertThat(resultado.valorConsolidado()).isEqualByComparingTo(new BigDecimal("1000.00"));

        ClienteAgrupamentoDTO clienteAgrupado = resultado.agrupamentoCliente().get(0);
        assertThat(clienteAgrupado.clienteId()).isEqualTo(1L);
        assertThat(clienteAgrupado.valorAportado()).isEqualByComparingTo(new BigDecimal("1000.00"));

        verify(clientesFeignClient, times(1)).listarClientesAtivos();
    }

    @Test
    @DisplayName("Deve retornar agrupamento vazio quando não há clientes ativos")
    void deveRetornarAgrupamentoVazioQuandoNaoHaClientesAtivos() {
        // Arrange
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(Collections.emptyList());

        // Act
        AgrupamentoResponseDTO resultado = clienteService.agruparClientes();

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.agrupamentoCliente()).isEmpty();
        assertThat(resultado.valorConsolidado()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(clientesFeignClient, times(1)).listarClientesAtivos();
    }

    @Test
    @DisplayName("Deve calcular corretamente valor aportado com arredondamento HALF_UP")
    void deveCalcularValorAportadoComArredondamento() {
        // Arrange - Cliente com valor que gera dízima periódica
        ClienteResponseDTO clienteComDizima = new ClienteResponseDTO(
                4L,
                "Ana Costa",
                "55544433322",
                "ana@email.com",
                new BigDecimal("1000.00"), // 1000 / 3 = 333.333...
                true,
                LocalDateTime.now(),
                new ContaGraficaDTO(4L, "FLH-004", "FILHOTE", LocalDateTime.now())
        );

        when(clientesFeignClient.listarClientesAtivos()).thenReturn(Collections.singletonList(clienteComDizima));

        // Act
        AgrupamentoResponseDTO resultado = clienteService.agruparClientes();

        // Assert
        assertThat(resultado.agrupamentoCliente().get(0).valorAportado())
                .isEqualByComparingTo(new BigDecimal("333.33")); // Arredondado para cima (HALF_UP)

        verify(clientesFeignClient, times(1)).listarClientesAtivos();
    }

    @Test
    @DisplayName("Deve somar corretamente valores consolidados de múltiplos clientes")
    void deveSomarCorretamenteValoresConsolidados() {
        // Arrange
        List<ClienteResponseDTO> clientesAtivos = Arrays.asList(cliente1, cliente2, cliente3);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(clientesAtivos);

        // Act
        AgrupamentoResponseDTO resultado = clienteService.agruparClientes();

        // Assert
        // (3000 + 6000 + 1500) / 3 = 10500 / 3 = 3500
        BigDecimal valorEsperado = new BigDecimal("1000.00")
                .add(new BigDecimal("2000.00"))
                .add(new BigDecimal("500.00"));

        assertThat(resultado.valorConsolidado()).isEqualByComparingTo(valorEsperado);

        verify(clientesFeignClient, times(1)).listarClientesAtivos();
    }

    @Test
    @DisplayName("Deve manter ordem dos clientes no agrupamento")
    void deveManterOrdemDosClientesNoAgrupamento() {
        // Arrange
        List<ClienteResponseDTO> clientesAtivos = Arrays.asList(cliente1, cliente2, cliente3);
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(clientesAtivos);

        // Act
        AgrupamentoResponseDTO resultado = clienteService.agruparClientes();

        // Assert
        assertThat(resultado.agrupamentoCliente().get(0).clienteId()).isEqualTo(1L);
        assertThat(resultado.agrupamentoCliente().get(1).clienteId()).isEqualTo(2L);
        assertThat(resultado.agrupamentoCliente().get(2).clienteId()).isEqualTo(3L);

        verify(clientesFeignClient, times(1)).listarClientesAtivos();
    }

    @Test
    @DisplayName("Deve mapear corretamente todos os campos do cliente")
    void deveMaperarCorretamenteTodosOsCamposDoCliente() {
        // Arrange
        when(clientesFeignClient.listarClientesAtivos()).thenReturn(Collections.singletonList(cliente1));

        // Act
        AgrupamentoResponseDTO resultado = clienteService.agruparClientes();

        // Assert
        ClienteAgrupamentoDTO clienteAgrupado = resultado.agrupamentoCliente().get(0);

        assertThat(clienteAgrupado.clienteId()).isEqualTo(cliente1.clienteId());
        assertThat(clienteAgrupado.nome()).isEqualTo(cliente1.nome());
        assertThat(clienteAgrupado.cpf()).isEqualTo(cliente1.cpf());
        assertThat(clienteAgrupado.contaId()).isEqualTo(cliente1.contaGrafica().id());
        assertThat(clienteAgrupado.valorAportado()).isNotNull();

        verify(clientesFeignClient, times(1)).listarClientesAtivos();
    }
}

