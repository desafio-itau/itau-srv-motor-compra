package com.itau.srv.motor.compras.dto.custodia;

import com.itau.srv.motor.compras.dto.conta.ContaMasterResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Resposta contendo informações da custódia master (resíduos não distribuídos)")
public record CustodiaMasterResponseDTO(
        @Schema(description = "Informações da conta master")
        ContaMasterResponseDTO contaMaster,

        @Schema(description = "Lista de custódias de resíduos")
        List<CustodiaResponseDTO> custodia,

        @Schema(description = "Valor total dos resíduos", example = "677.50")
        BigDecimal valorTotalResiduo
) {
}
