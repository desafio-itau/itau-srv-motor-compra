package com.itau.srv.motor.compras.dto.ordemcompra;

public record DetalhesOrdemCompra(
        String tipo,
        String ticker,
        Integer quantidade
) {
}
