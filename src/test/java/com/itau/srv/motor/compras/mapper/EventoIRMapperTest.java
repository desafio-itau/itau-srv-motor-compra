package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.ir.IRDedoDuroEventDTO;
import com.itau.srv.motor.compras.model.EventoIR;
import com.itau.srv.motor.compras.model.enums.TipoIR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventoIRMapper - Testes Unitários")
class EventoIRMapperTest {

    private EventoIRMapper mapper;
    private IRDedoDuroEventDTO dto;

    @BeforeEach
    void setUp() {
        mapper = new EventoIRMapper();

        dto = new IRDedoDuroEventDTO(
                "IR_DEDO_DURO",
                1L,
                "12345678901",
                "PETR4",
                "COMPRA",
                100,
                new BigDecimal("35.50"),
                new BigDecimal("3550.00"),
                new BigDecimal("0.00005"),
                new BigDecimal("0.18"),
                LocalDateTime.of(2026, 3, 1, 10, 30, 0)
        );
    }

    @Test
    @DisplayName("Deve mapear DTO para EventoIR corretamente")
    void deveMaperarDtoParaEventoIRCorretamente() {
        // Act
        EventoIR resultado = mapper.mapearParaEventoIr(dto);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getValorIR()).isEqualByComparingTo(new BigDecimal("0.18"));
        assertThat(resultado.getValorBase()).isEqualByComparingTo(new BigDecimal("3550.00"));
        assertThat(resultado.getTipo()).isEqualTo(TipoIR.IR_DEDO_DURO);
        assertThat(resultado.getClienteId()).isEqualTo(1L);
        assertThat(resultado.getPublicadoKafka()).isTrue();
        assertThat(resultado.getDataEvento()).isEqualTo(LocalDateTime.of(2026, 3, 1, 10, 30, 0));
    }

    @Test
    @DisplayName("Deve mapear valor IR corretamente")
    void deveMaperarValorIRCorretamente() {
        // Act
        EventoIR resultado = mapper.mapearParaEventoIr(dto);

        // Assert
        assertThat(resultado.getValorIR()).isEqualByComparingTo(new BigDecimal("0.18"));
    }

    @Test
    @DisplayName("Deve mapear valor base (valor da operação) corretamente")
    void deveMaperarValorBaseCorretamente() {
        // Act
        EventoIR resultado = mapper.mapearParaEventoIr(dto);

        // Assert
        assertThat(resultado.getValorBase()).isEqualByComparingTo(new BigDecimal("3550.00"));
    }

    @Test
    @DisplayName("Deve mapear tipo IR_DEDO_DURO")
    void deveMaperarTipoIRDedoDuro() {
        // Act
        EventoIR resultado = mapper.mapearParaEventoIr(dto);

        // Assert
        assertThat(resultado.getTipo()).isEqualTo(TipoIR.IR_DEDO_DURO);
    }

    @Test
    @DisplayName("Deve mapear clienteId corretamente")
    void deveMaperarClienteIdCorretamente() {
        // Act
        EventoIR resultado = mapper.mapearParaEventoIr(dto);

        // Assert
        assertThat(resultado.getClienteId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve marcar como publicado no Kafka")
    void deveMarcarComoPublicadoNoKafka() {
        // Act
        EventoIR resultado = mapper.mapearParaEventoIr(dto);

        // Assert
        assertThat(resultado.getPublicadoKafka()).isTrue();
    }

    @Test
    @DisplayName("Deve mapear data do evento corretamente")
    void deveMaperarDataDoEventoCorretamente() {
        // Act
        EventoIR resultado = mapper.mapearParaEventoIr(dto);

        // Assert
        assertThat(resultado.getDataEvento()).isEqualTo(LocalDateTime.of(2026, 3, 1, 10, 30, 0));
    }

    @Test
    @DisplayName("Deve mapear evento com valores grandes")
    void deveMaperarEventoComValoresGrandes() {
        // Arrange
        IRDedoDuroEventDTO dtoGrande = new IRDedoDuroEventDTO(
                "IR_DEDO_DURO",
                2L,
                "98765432100",
                "VALE3",
                "COMPRA",
                1000,
                new BigDecimal("62.50"),
                new BigDecimal("62500.00"),
                new BigDecimal("0.00005"),
                new BigDecimal("3.13"),
                LocalDateTime.now()
        );

        // Act
        EventoIR resultado = mapper.mapearParaEventoIr(dtoGrande);

        // Assert
        assertThat(resultado.getValorIR()).isEqualByComparingTo(new BigDecimal("3.13"));
        assertThat(resultado.getValorBase()).isEqualByComparingTo(new BigDecimal("62500.00"));
        assertThat(resultado.getClienteId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Deve mapear evento com valores pequenos")
    void deveMaperarEventoComValoresPequenos() {
        // Arrange
        IRDedoDuroEventDTO dtoPequeno = new IRDedoDuroEventDTO(
                "IR_DEDO_DURO",
                3L,
                "11122233344",
                "WEGE3",
                "COMPRA",
                1,
                new BigDecimal("40.00"),
                new BigDecimal("40.00"),
                new BigDecimal("0.00005"),
                new BigDecimal("0.00"),
                LocalDateTime.now()
        );

        // Act
        EventoIR resultado = mapper.mapearParaEventoIr(dtoPequeno);

        // Assert
        assertThat(resultado.getValorIR()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(resultado.getValorBase()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(resultado.getClienteId()).isEqualTo(3L);
    }
}

