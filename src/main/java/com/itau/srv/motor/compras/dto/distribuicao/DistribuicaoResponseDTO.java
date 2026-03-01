package com.itau.srv.motor.compras.dto.distribuicao;

import com.itau.srv.motor.compras.dto.ativo.AtivoResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record DistribuicaoResponseDTO(
        Long clienteId,
        String nome,
        BigDecimal valorAporte,
        List<AtivoResponseDTO> ativos
) {
}
