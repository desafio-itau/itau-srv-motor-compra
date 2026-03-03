package com.itau.srv.motor.compras.dto.valor;

import java.math.BigDecimal;

public record ValoresResponseDTO(
        BigDecimal valorInvestido,
        BigDecimal valorVendido
) {
}
