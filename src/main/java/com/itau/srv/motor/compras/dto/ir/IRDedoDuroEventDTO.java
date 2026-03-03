package com.itau.srv.motor.compras.dto.ir;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IRDedoDuroEventDTO(
        String tipo,
        Long clienteId,
        String cpf,
        String ticker,
        String tipoOperacao,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal valorOperacao,
        BigDecimal aliquota,
        BigDecimal valorIR,
        LocalDateTime dataOperacao
) {
}

