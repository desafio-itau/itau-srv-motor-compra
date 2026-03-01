package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.distribuicao.DistribuicaoResponseDTO;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.model.Distribuicao;
import com.itau.srv.motor.compras.model.OrdemCompra;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DistribuicaoMapper {

    public Distribuicao mapearParaDistribuicao(DistribuicaoResponseDTO dto, Custodia custodia, OrdemCompra ordemCompra) {
        Distribuicao distribuicao = new Distribuicao();

        distribuicao.setCustodia(custodia);
        distribuicao.setOrdemCompra(ordemCompra);
        distribuicao.setTicker(ordemCompra.getTicker());
        distribuicao.setDataDistribuicao(LocalDateTime.now());
        distribuicao.setQuantidade(ordemCompra.getQuantidade());
        distribuicao.setPrecoUnitario(ordemCompra.getPrecoUnitario());

        return distribuicao;
    }
}
