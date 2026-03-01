package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.distribuicao.DistribuicaoResponseDTO;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.model.Distribuicao;
import com.itau.srv.motor.compras.model.OrdemCompra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DistribuicaoMapper - Testes Unitários")
class DistribuicaoMapperTest {

    private DistribuicaoMapper mapper;
    private Custodia custodia;
    private OrdemCompra ordemCompra;
    private DistribuicaoResponseDTO dto;

    @BeforeEach
    void setUp() {
        mapper = new DistribuicaoMapper();

        custodia = new Custodia();
        custodia.setId(1L);
        custodia.setTicker("PETR4");
        custodia.setQuantidade(100);
        custodia.setPrecoMedio(new BigDecimal("35.00"));
        custodia.setContaGraficaId(10L);

        ordemCompra = new OrdemCompra();
        ordemCompra.setId(1L);
        ordemCompra.setTicker("PETR4");
        ordemCompra.setQuantidade(100);
        ordemCompra.setPrecoUnitario(new BigDecimal("35.50"));
        ordemCompra.setDataExecucao(LocalDateTime.now());

        dto = new DistribuicaoResponseDTO(
                1L,
                "Cliente Teste",
                new BigDecimal("1000.00"),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Deve mapear DTO para Distribuicao corretamente")
    void deveMaperarDtoParaDistribuicaoCorretamente() {
        // Act
        Distribuicao resultado = mapper.mapearParaDistribuicao(dto, custodia, ordemCompra);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getCustodia()).isEqualTo(custodia);
        assertThat(resultado.getOrdemCompra()).isEqualTo(ordemCompra);
        assertThat(resultado.getTicker()).isEqualTo("PETR4");
        assertThat(resultado.getQuantidade()).isEqualTo(100);
        assertThat(resultado.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("35.50"));
        assertThat(resultado.getDataDistribuicao()).isNotNull();
    }

    @Test
    @DisplayName("Deve usar ticker da ordem de compra")
    void deveUsarTickerDaOrdemDeCompra() {
        // Arrange
        ordemCompra.setTicker("VALE3");

        // Act
        Distribuicao resultado = mapper.mapearParaDistribuicao(dto, custodia, ordemCompra);

        // Assert
        assertThat(resultado.getTicker()).isEqualTo("VALE3");
    }

    @Test
    @DisplayName("Deve usar quantidade da ordem de compra")
    void deveUsarQuantidadeDaOrdemDeCompra() {
        // Arrange
        ordemCompra.setQuantidade(250);

        // Act
        Distribuicao resultado = mapper.mapearParaDistribuicao(dto, custodia, ordemCompra);

        // Assert
        assertThat(resultado.getQuantidade()).isEqualTo(250);
    }

    @Test
    @DisplayName("Deve usar preço unitário da ordem de compra")
    void deveUsarPrecoUnitarioDaOrdemDeCompra() {
        // Arrange
        ordemCompra.setPrecoUnitario(new BigDecimal("62.75"));

        // Act
        Distribuicao resultado = mapper.mapearParaDistribuicao(dto, custodia, ordemCompra);

        // Assert
        assertThat(resultado.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("62.75"));
    }

    @Test
    @DisplayName("Deve definir data de distribuição como now")
    void deveDefinirDataDistribuicaoComoNow() {
        // Arrange
        LocalDateTime antes = LocalDateTime.now();

        // Act
        Distribuicao resultado = mapper.mapearParaDistribuicao(dto, custodia, ordemCompra);

        // Assert
        LocalDateTime depois = LocalDateTime.now();
        assertThat(resultado.getDataDistribuicao()).isBetween(antes, depois);
    }

    @Test
    @DisplayName("Deve associar custodia corretamente")
    void deveAssociarCustodiaCorretamente() {
        // Act
        Distribuicao resultado = mapper.mapearParaDistribuicao(dto, custodia, ordemCompra);

        // Assert
        assertThat(resultado.getCustodia()).isSameAs(custodia);
    }

    @Test
    @DisplayName("Deve associar ordem de compra corretamente")
    void deveAssociarOrdemDeCompraCorretamente() {
        // Act
        Distribuicao resultado = mapper.mapearParaDistribuicao(dto, custodia, ordemCompra);

        // Assert
        assertThat(resultado.getOrdemCompra()).isSameAs(ordemCompra);
    }
}

