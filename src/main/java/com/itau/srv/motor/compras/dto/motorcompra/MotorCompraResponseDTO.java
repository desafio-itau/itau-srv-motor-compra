package com.itau.srv.motor.compras.dto.motorcompra;

import com.itau.srv.motor.compras.dto.distribuicao.DistribuicaoResponseDTO;
import com.itau.srv.motor.compras.dto.ordemcompra.OrdemCompraResponseDTO;
import com.itau.srv.motor.compras.dto.residuo.ResiduoContaMasterDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MotorCompraResponseDTO(
        LocalDateTime dataExecucao,
        Integer totalClientes,
        BigDecimal totalConsolidado,
        List<OrdemCompraResponseDTO> ordensCompra,
        List<DistribuicaoResponseDTO> distribuicoes,
        List<ResiduoContaMasterDTO> residuosCustMaster,
        Integer eventosIRPublicados,
        String mensagem
) {
}
