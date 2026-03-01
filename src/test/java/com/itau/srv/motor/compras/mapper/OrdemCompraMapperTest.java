package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.ordemcompra.DetalhesOrdemCompra;
import com.itau.srv.motor.compras.dto.ordemcompra.OrdemCompraResponseDTO;
import com.itau.srv.motor.compras.model.OrdemCompra;
import com.itau.srv.motor.compras.model.enums.TipoMercado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrdemCompraMapper - Testes Unitários")
class OrdemCompraMapperTest {

    private OrdemCompraMapper mapper;
    private OrdemCompraResponseDTO dto;
    private LocalDate dataReferencia;

    @BeforeEach
    void setUp() {
        mapper = new OrdemCompraMapper();

        List<DetalhesOrdemCompra> detalhes = List.of(
                new DetalhesOrdemCompra("LOTE", "PETR4", 100)
        );

        dto = new OrdemCompraResponseDTO(
                "PETR4",
                100,
                detalhes,
                new BigDecimal("35.50"),
                new BigDecimal("3550.00")
        );

        dataReferencia = LocalDate.now();
    }

    @Test
    @DisplayName("Deve mapear DTO para OrdemCompra corretamente")
    void deveMaperarDtoParaOrdemCompraCorretamente() {
        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dto, 1L, dataReferencia);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getTicker()).isEqualTo("PETR4");
        assertThat(resultado.getQuantidade()).isEqualTo(100);
        assertThat(resultado.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("35.50"));
        assertThat(resultado.getContaMasterId()).isEqualTo(1L);
        assertThat(resultado.getTipoMercado()).isEqualTo(TipoMercado.LOTE);
    }

    @Test
    @DisplayName("Deve mapear ticker corretamente")
    void deveMaperarTickerCorretamente() {
        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dto, 1L, dataReferencia);

        // Assert
        assertThat(resultado.getTicker()).isEqualTo("PETR4");
    }

    @Test
    @DisplayName("Deve mapear quantidade total corretamente")
    void deveMaperarQuantidadeTotalCorretamente() {
        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dto, 1L, dataReferencia);

        // Assert
        assertThat(resultado.getQuantidade()).isEqualTo(100);
    }

    @Test
    @DisplayName("Deve mapear preço unitário corretamente")
    void deveMaperarPrecoUnitarioCorretamente() {
        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dto, 1L, dataReferencia);

        // Assert
        assertThat(resultado.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("35.50"));
    }

    @Test
    @DisplayName("Deve mapear conta master ID corretamente")
    void deveMaperarContaMasterIdCorretamente() {
        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dto, 999L, dataReferencia);

        // Assert
        assertThat(resultado.getContaMasterId()).isEqualTo(999L);
    }

    @Test
    @DisplayName("Deve mapear tipo mercado LOTE")
    void deveMaperarTipoMercadoLote() {
        // Arrange
        List<DetalhesOrdemCompra> detalhesLote = List.of(
                new DetalhesOrdemCompra("LOTE", "PETR4", 100)
        );
        OrdemCompraResponseDTO dtoLote = new OrdemCompraResponseDTO(
                "PETR4", 100, detalhesLote, new BigDecimal("35.50"), new BigDecimal("3550.00")
        );

        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dtoLote, 1L, dataReferencia);

        // Assert
        assertThat(resultado.getTipoMercado()).isEqualTo(TipoMercado.LOTE);
    }

    @Test
    @DisplayName("Deve mapear tipo mercado FRACIONARIO")
    void deveMaperarTipoMercadoFracionario() {
        // Arrange
        List<DetalhesOrdemCompra> detalhesFracionario = List.of(
                new DetalhesOrdemCompra("FRACIONARIO", "PETR4F", 50)
        );
        OrdemCompraResponseDTO dtoFracionario = new OrdemCompraResponseDTO(
                "PETR4", 50, detalhesFracionario, new BigDecimal("35.50"), new BigDecimal("1775.00")
        );

        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dtoFracionario, 1L, dataReferencia);

        // Assert
        assertThat(resultado.getTipoMercado()).isEqualTo(TipoMercado.FRACIONARIO);
    }

    @Test
    @DisplayName("Deve mapear ordem com múltiplos detalhes usando primeiro detalhe")
    void deveMaperarOrdemComMultiplosDetalhesUsandoPrimeiroDetalhe() {
        // Arrange
        List<DetalhesOrdemCompra> multiplosDetalhes = List.of(
                new DetalhesOrdemCompra("LOTE", "PETR4", 100),
                new DetalhesOrdemCompra("FRACIONARIO", "PETR4F", 50)
        );
        OrdemCompraResponseDTO dtoMultiplo = new OrdemCompraResponseDTO(
                "PETR4", 150, multiplosDetalhes, new BigDecimal("35.50"), new BigDecimal("5325.00")
        );

        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dtoMultiplo, 1L, dataReferencia);

        // Assert
        // Deve usar o tipo do primeiro detalhe
        assertThat(resultado.getTipoMercado()).isEqualTo(TipoMercado.LOTE);
        assertThat(resultado.getQuantidade()).isEqualTo(150);
    }

    @Test
    @DisplayName("Deve mapear ordem com valores decimais")
    void deveMaperarOrdemComValoresDecimais() {
        // Arrange
        List<DetalhesOrdemCompra> detalhes = List.of(
                new DetalhesOrdemCompra("FRACIONARIO", "VALE3F", 15)
        );
        OrdemCompraResponseDTO dtoDecimal = new OrdemCompraResponseDTO(
                "VALE3", 15, detalhes, new BigDecimal("62.75"), new BigDecimal("941.25")
        );

        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dtoDecimal, 1L, dataReferencia);

        // Assert
        assertThat(resultado.getPrecoUnitario()).isEqualByComparingTo(new BigDecimal("62.75"));
    }

    @Test
    @DisplayName("Deve mapear diferentes tickers")
    void deveMaperarDiferentesTickers() {
        // Arrange
        List<DetalhesOrdemCompra> detalhesVale = List.of(
                new DetalhesOrdemCompra("LOTE", "VALE3", 100)
        );
        OrdemCompraResponseDTO dtoVale = new OrdemCompraResponseDTO(
                "VALE3", 100, detalhesVale, new BigDecimal("62.00"), new BigDecimal("6200.00")
        );

        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dtoVale, 1L, dataReferencia);

        // Assert
        assertThat(resultado.getTicker()).isEqualTo("VALE3");
    }

    @Test
    @DisplayName("Deve mapear ordem com quantidade pequena")
    void deveMaperarOrdemComQuantidadePequena() {
        // Arrange
        List<DetalhesOrdemCompra> detalhes = List.of(
                new DetalhesOrdemCompra("FRACIONARIO", "WEGE3F", 1)
        );
        OrdemCompraResponseDTO dtoPequeno = new OrdemCompraResponseDTO(
                "WEGE3", 1, detalhes, new BigDecimal("40.00"), new BigDecimal("40.00")
        );

        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dtoPequeno, 1L, dataReferencia);

        // Assert
        assertThat(resultado.getQuantidade()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve mapear ordem com quantidade grande")
    void deveMaperarOrdemComQuantidadeGrande() {
        // Arrange
        List<DetalhesOrdemCompra> detalhes = List.of(
                new DetalhesOrdemCompra("LOTE", "ITUB4", 500)
        );
        OrdemCompraResponseDTO dtoGrande = new OrdemCompraResponseDTO(
                "ITUB4", 500, detalhes, new BigDecimal("30.00"), new BigDecimal("15000.00")
        );

        // Act
        OrdemCompra resultado = mapper.mapearParaOrdemCompra(dtoGrande, 1L, dataReferencia);

        // Assert
        assertThat(resultado.getQuantidade()).isEqualTo(500);
    }
}

