package com.itau.srv.motor.compras.dto.ordemcompra;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detalhes de uma ordem de compra (lote padrão ou fracionário)")
public record DetalhesOrdemCompra(
        @Schema(description = "Tipo de lote", example = "LOTE_PADRAO", allowableValues = {"LOTE_PADRAO", "FRACIONARIO"})
        String tipo,

        @Schema(description = "Código do ativo (ticker com sufixo F para fracionário)", example = "PETR4")
        String ticker,

        @Schema(description = "Quantidade de ações", example = "300")
        Integer quantidade
) {
}
