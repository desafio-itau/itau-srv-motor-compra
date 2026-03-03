package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.ir.EventoIRSumarioDTO;
import com.itau.srv.motor.compras.dto.historico.HistoricoAportesResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HistoricoMapper - Testes Unitários")
class HistoricoMapperTest {

    private HistoricoMapper historicoMapper;

    @BeforeEach
    void setUp() {
        historicoMapper = new HistoricoMapper();
    }

    @Test
    @DisplayName("Deve mapear evento com data no início do mês para parcela 1/3")
    void deveMapearEventoComDataNoIniciodoMesParaParcela1_3() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 5),
                new BigDecimal("1000.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.data()).isEqualTo(LocalDate.of(2025, 3, 5));
        assertThat(resultado.valor()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(resultado.parcela()).isEqualTo("1/3");
    }

    @Test
    @DisplayName("Deve mapear evento com data no meio do mês para parcela 2/3")
    void deveMapearEventoComDataNoMeioDoMesParaParcela2_3() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 15),
                new BigDecimal("2000.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.data()).isEqualTo(LocalDate.of(2025, 3, 15));
        assertThat(resultado.valor()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(resultado.parcela()).isEqualTo("2/3");
    }

    @Test
    @DisplayName("Deve mapear evento com data no final do mês para parcela 3/3")
    void deveMapearEventoComDataNoFinalDoMesParaParcela3_3() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 25),
                new BigDecimal("1500.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.data()).isEqualTo(LocalDate.of(2025, 3, 25));
        assertThat(resultado.valor()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(resultado.parcela()).isEqualTo("3/3");
    }

    @Test
    @DisplayName("Deve mapear dia 1 para parcela 1/3")
    void deveMapearDia1ParaParcela1_3() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 1, 1),
                new BigDecimal("1000.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.parcela()).isEqualTo("1/3");
    }

    @Test
    @DisplayName("Deve mapear dia 10 para parcela 1/3")
    void deveMapearDia10ParaParcela1_3() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 1, 10),
                new BigDecimal("1000.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.parcela()).isEqualTo("1/3");
    }

    @Test
    @DisplayName("Deve mapear dia 11 para parcela 2/3")
    void deveMapearDia11ParaParcela2_3() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 1, 11),
                new BigDecimal("1000.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.parcela()).isEqualTo("2/3");
    }

    @Test
    @DisplayName("Deve mapear dia 20 para parcela 2/3")
    void deveMapearDia20ParaParcela2_3() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 1, 20),
                new BigDecimal("1000.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.parcela()).isEqualTo("2/3");
    }

    @Test
    @DisplayName("Deve mapear dia 21 para parcela 3/3")
    void deveMapearDia21ParaParcela3_3() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 1, 21),
                new BigDecimal("1000.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.parcela()).isEqualTo("3/3");
    }

    @Test
    @DisplayName("Deve mapear dia 31 para parcela 3/3")
    void deveMapearDia31ParaParcela3_3() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 1, 31),
                new BigDecimal("1000.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.parcela()).isEqualTo("3/3");
    }

    @Test
    @DisplayName("Deve mapear corretamente o valor base total de compras")
    void deveMapearCorretamenteOValorBaseTotalDeCompras() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 5),
                new BigDecimal("3500.50")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.valor()).isEqualByComparingTo(new BigDecimal("3500.50"));
    }

    @Test
    @DisplayName("Deve mapear valores decimais corretamente")
    void deveMapearValoresDecimaisCorretamente() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 5),
                new BigDecimal("1234.56")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.valor()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("Deve mapear valor zero corretamente")
    void deveMapearValorZeroCorretamente() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 5),
                BigDecimal.ZERO
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.valor()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.parcela()).isEqualTo("1/3");
    }

    @Test
    @DisplayName("Deve mapear diferentes meses corretamente")
    void deveMapearDiferentesMesesCorretamente() {
        // Arrange
        EventoIRSumarioDTO janeiro = new EventoIRSumarioDTO(
                LocalDate.of(2025, 1, 5),
                new BigDecimal("1000.00")
        );

        EventoIRSumarioDTO fevereiro = new EventoIRSumarioDTO(
                LocalDate.of(2025, 2, 15),
                new BigDecimal("2000.00")
        );

        EventoIRSumarioDTO marco = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 25),
                new BigDecimal("1500.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado1 = historicoMapper.mapearParaHistoricoAportes(janeiro);
        HistoricoAportesResponseDTO resultado2 = historicoMapper.mapearParaHistoricoAportes(fevereiro);
        HistoricoAportesResponseDTO resultado3 = historicoMapper.mapearParaHistoricoAportes(marco);

        // Assert
        assertThat(resultado1.data().getMonthValue()).isEqualTo(1);
        assertThat(resultado1.parcela()).isEqualTo("1/3");

        assertThat(resultado2.data().getMonthValue()).isEqualTo(2);
        assertThat(resultado2.parcela()).isEqualTo("2/3");

        assertThat(resultado3.data().getMonthValue()).isEqualTo(3);
        assertThat(resultado3.parcela()).isEqualTo("3/3");
    }

    @Test
    @DisplayName("Deve preservar a data original do evento")
    void devePreservarADataOriginalDoEvento() {
        // Arrange
        LocalDate dataOriginal = LocalDate.of(2025, 6, 18);
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                dataOriginal,
                new BigDecimal("1000.00")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.data()).isEqualTo(dataOriginal);
    }

    @Test
    @DisplayName("Deve mapear valores grandes corretamente")
    void deveMapearValoresGrandesCorretamente() {
        // Arrange
        EventoIRSumarioDTO eventoIR = new EventoIRSumarioDTO(
                LocalDate.of(2025, 3, 5),
                new BigDecimal("999999.99")
        );

        // Act
        HistoricoAportesResponseDTO resultado = historicoMapper.mapearParaHistoricoAportes(eventoIR);

        // Assert
        assertThat(resultado.valor()).isEqualByComparingTo(new BigDecimal("999999.99"));
    }
}

