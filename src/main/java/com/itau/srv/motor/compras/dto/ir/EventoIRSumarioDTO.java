package com.itau.srv.motor.compras.dto.ir;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventoIRSumarioDTO(
        LocalDate data,
        BigDecimal valorBaseTotalCompras
) {
}

