package com.itau.srv.motor.compras.dto.ordemcompra;

import java.math.BigDecimal;
import java.util.List;

public record OrdemCompraResponseDTO(
        String ticker,
        Integer quantidadeTotal,
        List<DetalhesOrdemCompra> detalhes,
        BigDecimal precoUnitario,
        BigDecimal valorTotal
) {
}
