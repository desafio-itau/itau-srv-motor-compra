package com.itau.srv.motor.compras.dto.ordemcompra;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Resposta contendo detalhes de uma ordem de compra executada")
public record OrdemCompraResponseDTO(
        @Schema(description = "Código do ativo (ticker)", example = "PETR4")
        String ticker,

        @Schema(description = "Quantidade total comprada", example = "350")
        Integer quantidadeTotal,

        @Schema(description = "Detalhes da ordem (lote padrão e fracionário)")
        List<DetalhesOrdemCompra> detalhes,

        @Schema(description = "Preço unitário da ação", example = "35.50")
        BigDecimal precoUnitario,

        @Schema(description = "Valor total da ordem", example = "12425.00")
        BigDecimal valorTotal
) {
}
