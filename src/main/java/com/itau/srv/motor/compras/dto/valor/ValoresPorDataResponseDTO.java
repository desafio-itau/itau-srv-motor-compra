package com.itau.srv.motor.compras.dto.valor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Resposta contendo valores investidos e vendidos em uma data específica")
public record ValoresPorDataResponseDTO(
        @Schema(description = "Data do evento", example = "2026-03-05")
        LocalDate dataEvento,

        @Schema(description = "Valores investidos e vendidos na data")
        ValoresResponseDTO valores
) {
}
