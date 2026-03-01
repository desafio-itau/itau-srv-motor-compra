package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.ordemcompra.OrdemCompraResponseDTO;
import com.itau.srv.motor.compras.model.OrdemCompra;
import com.itau.srv.motor.compras.model.enums.TipoMercado;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class OrdemCompraMapper {

    public OrdemCompra mapearParaOrdemCompra(OrdemCompraResponseDTO dto, Long contaMasterId, LocalDate dataReferencia) {
        OrdemCompra ordemCompra = new OrdemCompra();

        ordemCompra.setTicker(dto.ticker());
        ordemCompra.setQuantidade(dto.quantidadeTotal());
        ordemCompra.setPrecoUnitario(dto.precoUnitario());
        ordemCompra.setContaMasterId(contaMasterId);
        ordemCompra.setTipoMercado(TipoMercado.valueOf(dto.detalhes().get(0).tipo()));
        ordemCompra.setDataExecucao(dataReferencia.atTime(LocalTime.now()));

        return ordemCompra;
    }
}
