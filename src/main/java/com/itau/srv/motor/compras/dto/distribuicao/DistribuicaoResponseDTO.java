package com.itau.srv.motor.compras.dto.distribuicao;

import com.itau.srv.motor.compras.dto.ativo.AtivoResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Resposta contendo a distribuição de ativos para um cliente")
public record DistribuicaoResponseDTO(
        @Schema(description = "ID do cliente", example = "1")
        Long clienteId,

        @Schema(description = "Nome do cliente", example = "João Silva")
        String nome,

        @Schema(description = "Valor do aporte do cliente", example = "1000.00")
        BigDecimal valorAporte,

        @Schema(description = "Lista de ativos distribuídos ao cliente")
        List<AtivoResponseDTO> ativos
) {
}
