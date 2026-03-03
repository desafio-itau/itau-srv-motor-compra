package com.itau.srv.motor.compras.dto.rebalanceamento;

import java.math.BigDecimal;

public record OperacaoVendaDTO(
        Long clienteId,
        String cpf,
        String ticker,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal valorTotal,
        BigDecimal precoMedio,
        BigDecimal lucro
) {
}

