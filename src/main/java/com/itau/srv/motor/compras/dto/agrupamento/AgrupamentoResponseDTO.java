package com.itau.srv.motor.compras.dto.agrupamento;

import java.math.BigDecimal;
import java.util.List;

public record AgrupamentoResponseDTO(
        BigDecimal valorConsolidado,
        List<ClienteAgrupamentoDTO> agrupamentoCliente
) {
}
