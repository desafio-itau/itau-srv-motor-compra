package com.itau.srv.motor.compras.dto.itemcesta;

import java.math.BigDecimal;

public record ItemCotacaoAtualResponseDTO(
        String ticker,
        BigDecimal percentual,
        BigDecimal cotacaoAtual
) {
}