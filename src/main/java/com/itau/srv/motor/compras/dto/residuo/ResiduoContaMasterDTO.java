package com.itau.srv.motor.compras.dto.residuo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resíduo mantido na conta master (ações não distribuídas)")
public record ResiduoContaMasterDTO(
        @Schema(description = "Código do ativo (ticker)", example = "PETR4")
        String ticker,

        @Schema(description = "Quantidade de ações residuais", example = "2")
        Integer quantidade
) {
}
