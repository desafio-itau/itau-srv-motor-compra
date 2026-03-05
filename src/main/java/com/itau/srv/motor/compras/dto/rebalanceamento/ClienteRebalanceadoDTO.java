package com.itau.srv.motor.compras.dto.rebalanceamento;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Informações de um cliente rebalanceado")
public record ClienteRebalanceadoDTO(
        @Schema(description = "ID do cliente", example = "1")
        Long clienteId,

        @Schema(description = "Nome do cliente", example = "João Silva")
        String nome,

        @Schema(description = "Lista de operações realizadas")
        List<OperacaoRebalanceamentoDTO> operacoes
) {
}

