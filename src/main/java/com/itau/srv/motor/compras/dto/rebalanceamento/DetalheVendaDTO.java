package com.itau.srv.motor.compras.dto.rebalanceamento;

import java.math.BigDecimal;

public record DetalheVendaDTO(
        String ticker,
        Integer quantidade,
        BigDecimal precoVenda,
        BigDecimal precoMedio,
        BigDecimal lucro
) {
}

