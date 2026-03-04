package com.itau.srv.motor.compras.dto.conta;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informações da conta master")
public record ContaMasterResponseDTO(
        @Schema(description = "ID da conta", example = "1")
        Long id,

        @Schema(description = "Número da conta", example = "ITAUMASTER")
        String numeroConta,

        @Schema(description = "Tipo da conta", example = "MASTER")
        String tipo
) {
}
