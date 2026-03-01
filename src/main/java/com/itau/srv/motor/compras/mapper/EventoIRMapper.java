package com.itau.srv.motor.compras.mapper;

import com.itau.srv.motor.compras.dto.ir.IRDedoDuroEventDTO;
import com.itau.srv.motor.compras.model.EventoIR;
import com.itau.srv.motor.compras.model.enums.TipoIR;
import org.springframework.stereotype.Component;

@Component
public class EventoIRMapper {

    public EventoIR mapearParaEventoIr(IRDedoDuroEventDTO dto) {
        EventoIR eventoIr = new EventoIR();

        eventoIr.setValorIR(dto.valorIR());
        eventoIr.setValorBase(dto.valorOperacao());
        eventoIr.setTipo(TipoIR.valueOf(dto.tipo()));
        eventoIr.setClienteId(dto.clienteId());
        eventoIr.setPublicadoKafka(true);
        eventoIr.setDataEvento(dto.dataOperacao());

        return eventoIr;
    }
}
