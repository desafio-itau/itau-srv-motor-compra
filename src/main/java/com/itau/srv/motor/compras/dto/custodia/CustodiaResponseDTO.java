package com.itau.srv.motor.compras.dto.custodia;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resposta contendo informações de custódia de um ativo")
public record CustodiaResponseDTO(
        @Schema(description = "Código do ativo (ticker)", example = "PETR4")
        String ticker,

        @Schema(description = "Quantidade de ações do ativo", example = "100")
        Integer quantidade,

        @Schema(description = "Preço médio de aquisição", example = "35.50")
        BigDecimal precoMedio,

        @Schema(description = "Valor atual do ativo (cotação)", example = "36.00")
        BigDecimal valorAtual,

        @Schema(description = "Origem da custódia", example = "Resíduo distribuição 2026-03-01")
        String origem
) {
}
