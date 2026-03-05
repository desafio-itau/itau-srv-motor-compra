package com.itau.srv.motor.compras.dto.rebalanceamento;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request para rebalanceamento por desvio de proporção")
public record RebalanceamentoDesvioRequestDTO(
        @Schema(description = "Limiar de desvio em pontos percentuais", example = "5.0", defaultValue = "5.0")
        BigDecimal limiarDesvio,

        @Schema(description = "Data de execução do rebalanceamento", example = "2026-03-04")
        LocalDate dataExecucao
) {
    public RebalanceamentoDesvioRequestDTO {
        if (limiarDesvio == null) {
            limiarDesvio = new BigDecimal("5.0");
        }
    }
}

