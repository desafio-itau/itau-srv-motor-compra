package com.itau.srv.motor.compras.dto.motorcompra;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Request para execução do motor de compra")
public record MotorCompraRequestDTO(
        @Schema(
                description = "Data de referência para execução da compra. Deve ser um dia útil (segunda a sexta-feira)",
                example = "2026-03-05",
                required = true
        )
        LocalDate dataReferencia
) {
}
