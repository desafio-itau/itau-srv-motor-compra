package com.itau.srv.motor.compras.dto.valor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resposta contendo valores investidos e vendidos por um cliente")
public record ValoresResponseDTO(
        @Schema(description = "Valor total investido pelo cliente", example = "5000.00")
        BigDecimal valorInvestido,

        @Schema(description = "Valor total vendido pelo cliente", example = "1500.00")
        BigDecimal valorVendido
) {
}
