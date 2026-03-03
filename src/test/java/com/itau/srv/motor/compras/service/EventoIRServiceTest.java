package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.agrupamento.ClienteAgrupamentoDTO;
import com.itau.srv.motor.compras.dto.ir.IRDedoDuroEventDTO;
import com.itau.srv.motor.compras.kafka.IREventProducer;
import com.itau.srv.motor.compras.mapper.EventoIRMapper;
import com.itau.srv.motor.compras.model.EventoIR;
import com.itau.srv.motor.compras.repository.EventoIRRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventoIRService - Testes Unitários")
class EventoIRServiceTest {

    @Mock
    private IREventProducer irEventProducer;

    @Mock
    private EventoIRRepository eventoIRRepository;

    @Mock
    private EventoIRMapper eventoIRMapper;

    @InjectMocks
    private EventoIRService eventoIRService;

    private ClienteAgrupamentoDTO cliente;

    @BeforeEach
    void setUp() {
        cliente = new ClienteAgrupamentoDTO(
                1L,
                "João Silva",
                "12345678901",
                1L,
                new BigDecimal("1000.00")
        );
    }

    @Test
    @DisplayName("Deve publicar evento IR e salvar no banco com sucesso")
    void devePublicarEventoIRESalvarNoBanco() {
        // Arrange
        String ticker = "PETR4";
        int quantidade = 100;
        BigDecimal precoUnitario = new BigDecimal("35.50");
        EventoIR eventoIREntity = mock(EventoIR.class);

        when(eventoIRMapper.mapearParaEventoIr(any(IRDedoDuroEventDTO.class))).thenReturn(eventoIREntity);
        when(eventoIRRepository.save(any(EventoIR.class))).thenReturn(eventoIREntity);

        // Act
        eventoIRService.publicarEventoIR(cliente, ticker, quantidade, precoUnitario, LocalDate.now());

        // Assert
        verify(irEventProducer, times(1)).publicarEventoIRDedoDuro(any(IRDedoDuroEventDTO.class));
        verify(eventoIRMapper, times(1)).mapearParaEventoIr(any(IRDedoDuroEventDTO.class));
        verify(eventoIRRepository, times(1)).save(eventoIREntity);
    }

    @Test
    @DisplayName("Deve criar evento IR com dados corretos do cliente")
    void deveCriarEventoIRComDadosCorretosDoCliente() {
        // Arrange
        String ticker = "VALE3";
        int quantidade = 50;
        BigDecimal precoUnitario = new BigDecimal("62.00");

        ArgumentCaptor<IRDedoDuroEventDTO> eventoCaptor = ArgumentCaptor.forClass(IRDedoDuroEventDTO.class);
        EventoIR eventoIREntity = mock(EventoIR.class);

        when(eventoIRMapper.mapearParaEventoIr(any(IRDedoDuroEventDTO.class))).thenReturn(eventoIREntity);

        // Act
        eventoIRService.publicarEventoIR(cliente, ticker, quantidade, precoUnitario, LocalDate.now());

        // Assert
        verify(irEventProducer).publicarEventoIRDedoDuro(eventoCaptor.capture());

        IRDedoDuroEventDTO eventoCriado = eventoCaptor.getValue();
        assertThat(eventoCriado.clienteId()).isEqualTo(1L);
        assertThat(eventoCriado.cpf()).isEqualTo("12345678901");
        assertThat(eventoCriado.ticker()).isEqualTo("VALE3");
        assertThat(eventoCriado.quantidade()).isEqualTo(50);
        assertThat(eventoCriado.precoUnitario()).isEqualByComparingTo(new BigDecimal("62.00"));
    }

    @Test
    @DisplayName("Deve calcular corretamente valor da operação")
    void deveCalcularCorretamenteValorDaOperacao() {
        // Arrange
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "PETR4",
                100,
                new BigDecimal("35.50"),
                LocalDate.now()
        );

        // Assert
        // 100 × 35.50 = 3550.00
        assertThat(evento.valorOperacao()).isEqualByComparingTo(new BigDecimal("3550.00"));
    }

    @Test
    @DisplayName("Deve calcular corretamente valor do IR (0,005%)")
    void deveCalcularCorretamenteValorDoIR() {
        // Arrange
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "PETR4",
                100,
                new BigDecimal("35.50"),
                LocalDate.now()
        );

        // Assert
        // Valor operação: 100 × 35.50 = 3550.00
        // IR: 3550.00 × 0.00005 = 0.1775 → arredondado para 0.18 (HALF_UP)
        assertThat(evento.valorIR()).isEqualByComparingTo(new BigDecimal("0.18"));
    }

    @Test
    @DisplayName("Deve calcular IR corretamente para valores pequenos")
    void deveCalcularIRCorretamenteParaValoresPequenos() {
        // Arrange
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "WEGE3",
                10,
                new BigDecimal("40.00"),
                LocalDate.now()
        );

        // Assert
        // Valor operação: 10 × 40.00 = 400.00
        // IR: 400.00 × 0.00005 = 0.02
        assertThat(evento.valorOperacao()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(evento.valorIR()).isEqualByComparingTo(new BigDecimal("0.02"));
    }

    @Test
    @DisplayName("Deve calcular IR corretamente para valores grandes")
    void deveCalcularIRCorretamenteParaValoresGrandes() {
        // Arrange
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "VALE3",
                1000,
                new BigDecimal("62.50"),
                LocalDate.now()
        );

        // Assert
        // Valor operação: 1000 × 62.50 = 62500.00
        // IR: 62500.00 × 0.00005 = 3.125 → arredondado para 3.13 (HALF_UP)
        assertThat(evento.valorOperacao()).isEqualByComparingTo(new BigDecimal("62500.00"));
        assertThat(evento.valorIR()).isEqualByComparingTo(new BigDecimal("3.13"));
    }

    @Test
    @DisplayName("Deve criar evento com alíquota correta (0,00005)")
    void deveCriarEventoComAliquotaCorreta() {
        // Arrange
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "PETR4",
                100,
                new BigDecimal("35.50"),
                LocalDate.now()
        );

        // Assert
        assertThat(evento.aliquota()).isEqualByComparingTo(new BigDecimal("0.00005"));
    }

    @Test
    @DisplayName("Deve criar evento com tipo IR_DEDO_DURO")
    void deveCriarEventoComTipoIRDedoDuro() {
        // Arrange
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "PETR4",
                100,
                new BigDecimal("35.50"),
                LocalDate.now()
        );

        // Assert
        assertThat(evento.tipo()).isEqualTo("IR_DEDO_DURO");
    }

    @Test
    @DisplayName("Deve criar evento com tipo de operação COMPRA")
    void deveCriarEventoComTipoOperacaoCompra() {
        // Arrange
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "PETR4",
                100,
                new BigDecimal("35.50"),
                LocalDate.now()
        );

        // Assert
        assertThat(evento.tipoOperacao()).isEqualTo("COMPRA");
    }

    @Test
    @DisplayName("Deve criar evento com data/hora atual")
    void deveCriarEventoComDataHoraAtual() {
        // Arrange
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "PETR4",
                100,
                new BigDecimal("35.50"),
                LocalDate.now()
        );

        // Assert
        assertThat(evento.dataOperacao()).isNotNull();
        assertThat(evento.dataOperacao()).isBeforeOrEqualTo(java.time.LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve arredondar IR com 2 casas decimais usando HALF_UP")
    void deveArredondarIRComDuasCasasDecimaisUsandoHalfUp() {
        // Arrange - Valor que gera dízima periódica
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "ITUB4",
                333,
                new BigDecimal("30.00"),
                LocalDate.now()
        );

        // Assert
        // Valor operação: 333 × 30.00 = 9990.00
        // IR: 9990.00 × 0.00005 = 0.4995 → arredondado para 0.50 (HALF_UP)
        assertThat(evento.valorIR()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(evento.valorIR().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve publicar evento IR para múltiplos ativos do mesmo cliente")
    void devePublicarEventoIRParaMultiplosAtivos() {
        // Arrange
        EventoIR eventoIREntity = mock(EventoIR.class);
        when(eventoIRMapper.mapearParaEventoIr(any(IRDedoDuroEventDTO.class))).thenReturn(eventoIREntity);

        // Act
        eventoIRService.publicarEventoIR(cliente, "PETR4", 100, new BigDecimal("35.50"), LocalDate.now());
        eventoIRService.publicarEventoIR(cliente, "VALE3", 50, new BigDecimal("62.00"), LocalDate.now());
        eventoIRService.publicarEventoIR(cliente, "ITUB4", 200, new BigDecimal("30.00"), LocalDate.now());

        // Assert
        verify(irEventProducer, times(3)).publicarEventoIRDedoDuro(any(IRDedoDuroEventDTO.class));
        verify(eventoIRRepository, times(3)).save(any(EventoIR.class));
    }

    @Test
    @DisplayName("Deve criar evento mesmo com quantidade mínima (1 ação)")
    void deveCriarEventoComQuantidadeMinima() {
        // Arrange
        IRDedoDuroEventDTO evento = EventoIRService.criarParaCompra(
                1L,
                "12345678901",
                "WEGE3",
                1,
                new BigDecimal("40.00"),
                LocalDate.now()
        );

        // Assert
        assertThat(evento.quantidade()).isEqualTo(1);
        assertThat(evento.valorOperacao()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(evento.valorIR()).isEqualByComparingTo(new BigDecimal("0.00")); // 0.002 → 0.00
    }
}

