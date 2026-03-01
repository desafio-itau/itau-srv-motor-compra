package com.itau.srv.motor.compras.dto.ir;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IRDedoDuroEventDTO(
        String tipo,                    // "IR_DEDO_DURO"
        Long clienteId,                 // ID do cliente
        String cpf,                     // CPF do cliente
        String ticker,                  // Ticker do ativo (ex: PETR4)
        String tipoOperacao,            // "COMPRA" ou "VENDA"
        Integer quantidade,             // Quantidade de ações
        BigDecimal precoUnitario,       // Preço unitário da ação
        BigDecimal valorOperacao,       // Valor total da operação (quantidade × preço)
        BigDecimal aliquota,            // 0,00005 (0,005%)
        BigDecimal valorIR,             // Valor do IR calculado
        LocalDateTime dataOperacao      // Data/hora da operação
) {
}

