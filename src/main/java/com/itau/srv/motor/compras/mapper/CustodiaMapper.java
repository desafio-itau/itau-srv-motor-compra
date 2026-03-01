package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.conta.ContaGraficaDTO;
import com.itau.srv.motor.compras.dto.conta.ContaMasterResponseDTO;
import com.itau.srv.motor.compras.dto.custodia.CustodiaMasterResponseDTO;
import com.itau.srv.motor.compras.dto.custodia.CustodiaResponseDTO;
import com.itau.srv.motor.compras.model.Custodia;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CustodiaMapper {

    public CustodiaMasterResponseDTO mapearParaCustodiaMasterResponseDTO(ContaGraficaDTO contaMaster, List<CustodiaResponseDTO> custodias, BigDecimal valorTotalResiduo) {
        return new CustodiaMasterResponseDTO(
                new ContaMasterResponseDTO(
                        contaMaster.id(),
                        contaMaster.numeroConta(),
                        contaMaster.tipoConta()
                ),
                custodias,
                valorTotalResiduo
        );
    }

    public CustodiaResponseDTO mapearParaCustodiaResponse(Custodia custodia, BigDecimal valorAtual) {
        return new CustodiaResponseDTO(
                custodia.getTicker(),
                custodia.getQuantidade(),
                custodia.getPrecoMedio(),
                valorAtual,
                "Resíduo distribuição " + custodia.getDataUltimaAtualizacao().toLocalDate()
        );
    }
}
