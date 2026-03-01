package com.itau.srv.motor.compras.dto.agrupamento;

import java.math.BigDecimal;

public record ClienteAgrupamentoDTO(
        Long clienteId,
        String nome,
        String cpf,
        Long contaId,
        BigDecimal valorAportado
) {
}
