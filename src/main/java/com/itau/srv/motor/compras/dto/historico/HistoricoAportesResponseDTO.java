package com.itau.srv.motor.compras.dto.historico;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HistoricoAportesResponseDTO(
        LocalDate data,
        BigDecimal valor,
        String parcela
) {
}
