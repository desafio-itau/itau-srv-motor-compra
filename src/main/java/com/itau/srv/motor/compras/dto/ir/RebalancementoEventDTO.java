package com.itau.srv.motor.compras.dto.ir;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Evento de rebalanceamento de cesta de ativos")
public record RebalancementoEventDTO(
        @Schema(description = "ID da cesta anterior", example = "1", required = true)
        Long cestaAnteriorId,

        @Schema(description = "ID da nova cesta ativa", example = "2", required = true)
        Long cestaAtualId,

        @Schema(description = "Data e hora da execução do rebalanceamento", example = "2026-03-05T10:00:00", required = true)
        LocalDateTime dataExecucao
) {
}

