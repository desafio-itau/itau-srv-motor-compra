package com.itau.srv.motor.compras.dto.historico;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Resposta contendo histórico de aportes realizados")
public record HistoricoAportesResponseDTO(
        @Schema(description = "Data do aporte", example = "2026-03-05")
        LocalDate data,

        @Schema(description = "Valor do aporte", example = "1000.00")
        BigDecimal valor,

        @Schema(description = "Parcela do aporte mensal", example = "1/3")
        String parcela
) {
}
