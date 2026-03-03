package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.ir.EventoIRSumarioDTO;
import com.itau.srv.motor.compras.dto.historico.HistoricoAportesResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class HistoricoMapper {

    public HistoricoAportesResponseDTO mapearParaHistoricoAportes(EventoIRSumarioDTO eventoIR) {
        StringBuilder builder = new StringBuilder();

        int dia = eventoIR.data().getDayOfMonth();

        if (dia <= 10) builder.append("1/3");
        else if (dia <= 20) builder.append("2/3");
        else builder.append("3/3");

        return new HistoricoAportesResponseDTO(
                eventoIR.data(),
                eventoIR.valorBaseTotalCompras(),
                builder.toString()
        );
    }
}
