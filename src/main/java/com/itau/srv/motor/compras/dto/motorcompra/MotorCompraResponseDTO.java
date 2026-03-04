package com.itau.srv.motor.compras.dto.motorcompra;

import com.itau.srv.motor.compras.dto.distribuicao.DistribuicaoResponseDTO;
import com.itau.srv.motor.compras.dto.ordemcompra.OrdemCompraResponseDTO;
import com.itau.srv.motor.compras.dto.residuo.ResiduoContaMasterDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Resposta da execução do motor de compra com detalhes das operações realizadas")
public record MotorCompraResponseDTO(
        @Schema(description = "Data e hora da execução", example = "2026-03-05T10:30:00")
        LocalDateTime dataExecucao,

        @Schema(description = "Total de clientes ativos processados", example = "3")
        Integer totalClientes,

        @Schema(description = "Valor total consolidado dos aportes", example = "3000.00")
        BigDecimal totalConsolidado,

        @Schema(description = "Lista de ordens de compra executadas")
        List<OrdemCompraResponseDTO> ordensCompra,

        @Schema(description = "Lista de distribuições realizadas para cada cliente")
        List<DistribuicaoResponseDTO> distribuicoes,

        @Schema(description = "Lista de resíduos mantidos na conta master")
        List<ResiduoContaMasterDTO> residuosCustMaster,

        @Schema(description = "Quantidade de eventos de IR publicados", example = "15")
        Integer eventosIRPublicados,

        @Schema(description = "Mensagem de conclusão", example = "Motor de compra executado com sucesso")
        String mensagem
) {
}
