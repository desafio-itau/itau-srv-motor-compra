package com.itau.srv.motor.compras.dto.ativo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informações de um ativo distribuído")
public record AtivoResponseDTO(
        @Schema(description = "Código do ativo (ticker)", example = "PETR4")
        String ticker,

        @Schema(description = "Quantidade de ações distribuídas", example = "35")
        Integer quantidade
) {
}
