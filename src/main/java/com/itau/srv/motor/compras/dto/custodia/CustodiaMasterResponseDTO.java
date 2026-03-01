package com.itau.srv.motor.compras.dto.custodia;

import com.itau.srv.motor.compras.dto.conta.ContaMasterResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record CustodiaMasterResponseDTO(
        ContaMasterResponseDTO contaMaster,
        List<CustodiaResponseDTO> custodia,
        BigDecimal valorTotalResiduo
) {
}
