package com.itau.srv.motor.compras.dto.ordemcompra;

import com.itau.srv.motor.compras.model.OrdemCompra;

import java.util.List;

public record CalcularQuantidadeAtivoResponse(
        List<OrdemCompra> entities,
        List<OrdemCompraResponseDTO> dtos
) {
}
