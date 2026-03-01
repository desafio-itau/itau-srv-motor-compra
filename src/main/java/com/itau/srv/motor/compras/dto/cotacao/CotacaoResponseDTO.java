package com.itau.srv.motor.compras.dto.cotacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CotacaoResponseDTO(
        LocalDate dataPregao,
        String ticker,
        Integer tipoMercado,
        BigDecimal precoAbertura,
        BigDecimal precoMaximo,
        BigDecimal precoMinimo,
        BigDecimal precoFechamento
) {
}
