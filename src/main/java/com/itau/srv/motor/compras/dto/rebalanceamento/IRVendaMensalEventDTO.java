package com.itau.srv.motor.compras.dto.rebalanceamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record IRVendaMensalEventDTO(
        String tipo,
        Long clienteId,
        String cpf,
        String mesReferencia,
        BigDecimal totalVendasMes,
        BigDecimal lucroLiquido,
        BigDecimal aliquota,
        BigDecimal valorIR,
        List<DetalheVendaDTO> detalhes,
        LocalDateTime dataCalculo
) {
}

