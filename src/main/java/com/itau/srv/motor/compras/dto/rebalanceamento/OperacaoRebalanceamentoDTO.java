package com.itau.srv.motor.compras.dto.rebalanceamento;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Operação de rebalanceamento realizada")
public record OperacaoRebalanceamentoDTO(
        @Schema(description = "Ticker do ativo", example = "PETR4")
        String ticker,

        @Schema(description = "Tipo de operação", example = "VENDA", allowableValues = {"COMPRA", "VENDA"})
        String tipoOperacao,

        @Schema(description = "Quantidade de ações", example = "10")
        Integer quantidade,

        @Schema(description = "Preço unitário", example = "35.50")
        BigDecimal precoUnitario,

        @Schema(description = "Valor total da operação", example = "355.00")
        BigDecimal valorTotal,

        @Schema(description = "Proporção antes do rebalanceamento", example = "32.5")
        BigDecimal proporcaoAntes,

        @Schema(description = "Proporção alvo (da cesta)", example = "30.0")
        BigDecimal proporcaoAlvo,

        @Schema(description = "Desvio em pontos percentuais", example = "2.5")
        BigDecimal desvio
) {
}

