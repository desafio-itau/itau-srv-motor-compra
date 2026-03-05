package com.itau.srv.motor.compras.dto.rebalanceamento;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta do rebalanceamento por desvio")
public record RebalanceamentoDesvioResponseDTO(
        @Schema(description = "Total de clientes processados", example = "10")
        Integer totalClientesProcessados,

        @Schema(description = "Total de clientes que precisaram rebalancear", example = "3")
        Integer totalClientesRebalanceados,

        @Schema(description = "Total de operações realizadas", example = "15")
        Integer totalOperacoes,

        @Schema(description = "Lista de clientes rebalanceados")
        List<ClienteRebalanceadoDTO> clientesRebalanceados,

        @Schema(description = "Mensagem de resultado", example = "Rebalanceamento por desvio executado com sucesso")
        String mensagem
) {
}

