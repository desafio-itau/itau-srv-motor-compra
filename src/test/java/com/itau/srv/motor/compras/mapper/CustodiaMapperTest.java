package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.conta.ContaGraficaDTO;
import com.itau.srv.motor.compras.dto.custodia.CustodiaMasterResponseDTO;
import com.itau.srv.motor.compras.dto.custodia.CustodiaResponseDTO;
import com.itau.srv.motor.compras.model.Custodia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustodiaMapper - Testes Unitários")
class CustodiaMapperTest {

    private CustodiaMapper custodiaMapper;

    @BeforeEach
    void setUp() {
        custodiaMapper = new CustodiaMapper();
    }

    @Test
    @DisplayName("Deve mapear para CustodiaMasterResponseDTO corretamente")
    void deveMapearParaCustodiaMasterResponseDTO() {
        // Arrange
        ContaGraficaDTO contaMaster = new ContaGraficaDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER",
                LocalDateTime.now()
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

        List<CustodiaResponseDTO> custodias = List.of(custodia1, custodia2);
        BigDecimal valorTotalResiduo = new BigDecimal("677.50");

        // Act
        CustodiaMasterResponseDTO resultado = custodiaMapper.mapearParaCustodiaMasterResponseDTO(
                contaMaster,
                custodias,
                valorTotalResiduo
        );

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.contaMaster()).isNotNull();
        assertThat(resultado.contaMaster().id()).isEqualTo(1L);
        assertThat(resultado.contaMaster().numeroConta()).isEqualTo("CONTA-MASTER-001");
        assertThat(resultado.contaMaster().tipo()).isEqualTo("MASTER");
        assertThat(resultado.custodia()).hasSize(2);
        assertThat(resultado.valorTotalResiduo()).isEqualByComparingTo(new BigDecimal("677.50"));
    }

    @Test
    @DisplayName("Deve mapear para CustodiaMasterResponseDTO com lista vazia")
    void deveMapearParaCustodiaMasterResponseDTOComListaVazia() {
        // Arrange
        ContaGraficaDTO contaMaster = new ContaGraficaDTO(
                1L,
                "CONTA-MASTER-001",
                "MASTER",
                LocalDateTime.now()
        );

        List<CustodiaResponseDTO> custodiasVazias = List.of();
        BigDecimal valorZero = BigDecimal.ZERO;

        // Act
        CustodiaMasterResponseDTO resultado = custodiaMapper.mapearParaCustodiaMasterResponseDTO(
                contaMaster,
                custodiasVazias,
                valorZero
        );

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.contaMaster()).isNotNull();
        assertThat(resultado.custodia()).isEmpty();
        assertThat(resultado.valorTotalResiduo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve mapear para CustodiaResponse corretamente")
    void deveMapearParaCustodiaResponse() {
        // Arrange
        Custodia custodia = new Custodia();
        custodia.setId(1L);
        custodia.setTicker("PETR4");
        custodia.setQuantidade(10);
        custodia.setPrecoMedio(new BigDecimal("35.50"));
        custodia.setContaGraficaId(100L);
        custodia.setDataUltimaAtualizacao(LocalDateTime.of(2026, 3, 1, 10, 0));

        BigDecimal valorAtual = new BigDecimal("36.00");

        // Act
        CustodiaResponseDTO resultado = custodiaMapper.mapearParaCustodiaResponse(custodia, valorAtual);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.ticker()).isEqualTo("PETR4");
        assertThat(resultado.quantidade()).isEqualTo(10);
        assertThat(resultado.precoMedio()).isEqualByComparingTo(new BigDecimal("35.50"));
        assertThat(resultado.valorAtual()).isEqualByComparingTo(new BigDecimal("36.00"));
        assertThat(resultado.origem()).isEqualTo("Resíduo distribuição 2026-03-01");
    }

    @Test
    @DisplayName("Deve mapear para CustodiaResponse com diferentes datas")
    void deveMapearParaCustodiaResponseComDiferentesDatas() {
        // Arrange
        Custodia custodia = new Custodia();
        custodia.setId(2L);
        custodia.setTicker("VALE3");
        custodia.setQuantidade(25);
        custodia.setPrecoMedio(new BigDecimal("62.00"));
        custodia.setContaGraficaId(100L);
        custodia.setDataUltimaAtualizacao(LocalDateTime.of(2026, 2, 15, 14, 30));

        BigDecimal valorAtual = new BigDecimal("63.50");

        // Act
        CustodiaResponseDTO resultado = custodiaMapper.mapearParaCustodiaResponse(custodia, valorAtual);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.ticker()).isEqualTo("VALE3");
        assertThat(resultado.quantidade()).isEqualTo(25);
        assertThat(resultado.precoMedio()).isEqualByComparingTo(new BigDecimal("62.00"));
        assertThat(resultado.valorAtual()).isEqualByComparingTo(new BigDecimal("63.50"));
        assertThat(resultado.origem()).isEqualTo("Resíduo distribuição 2026-02-15");
    }

    @Test
    @DisplayName("Deve mapear para CustodiaResponse com quantidade zero")
    void deveMapearParaCustodiaResponseComQuantidadeZero() {
        // Arrange
        Custodia custodia = new Custodia();
        custodia.setId(3L);
        custodia.setTicker("BBDC4");
        custodia.setQuantidade(0);
        custodia.setPrecoMedio(new BigDecimal("25.00"));
        custodia.setContaGraficaId(100L);
        custodia.setDataUltimaAtualizacao(LocalDateTime.of(2026, 3, 1, 10, 0));

        BigDecimal valorAtual = new BigDecimal("25.50");

        // Act
        CustodiaResponseDTO resultado = custodiaMapper.mapearParaCustodiaResponse(custodia, valorAtual);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.ticker()).isEqualTo("BBDC4");
        assertThat(resultado.quantidade()).isZero();
        assertThat(resultado.precoMedio()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(resultado.valorAtual()).isEqualByComparingTo(new BigDecimal("25.50"));
    }

    @Test
    @DisplayName("Deve mapear múltiplas custodias para response")
    void deveMapearMultiplasCustodiasParaResponse() {
        // Arrange
        Custodia custodia1 = new Custodia();
        custodia1.setTicker("PETR4");
        custodia1.setQuantidade(10);
        custodia1.setPrecoMedio(new BigDecimal("35.50"));
        custodia1.setDataUltimaAtualizacao(LocalDateTime.of(2026, 3, 1, 10, 0));

        Custodia custodia2 = new Custodia();
        custodia2.setTicker("VALE3");
        custodia2.setQuantidade(5);
        custodia2.setPrecoMedio(new BigDecimal("62.00"));
        custodia2.setDataUltimaAtualizacao(LocalDateTime.of(2026, 3, 1, 10, 0));

        // Act
        CustodiaResponseDTO resultado1 = custodiaMapper.mapearParaCustodiaResponse(custodia1, new BigDecimal("36.00"));
        CustodiaResponseDTO resultado2 = custodiaMapper.mapearParaCustodiaResponse(custodia2, new BigDecimal("63.50"));

        // Assert
        assertThat(resultado1.ticker()).isEqualTo("PETR4");
        assertThat(resultado1.quantidade()).isEqualTo(10);
        assertThat(resultado2.ticker()).isEqualTo("VALE3");
        assertThat(resultado2.quantidade()).isEqualTo(5);
    }
}

