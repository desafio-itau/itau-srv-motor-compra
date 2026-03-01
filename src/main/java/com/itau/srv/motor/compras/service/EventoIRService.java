package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.agrupamento.ClienteAgrupamentoDTO;
import com.itau.srv.motor.compras.dto.ir.IRDedoDuroEventDTO;
import com.itau.srv.motor.compras.kafka.IREventProducer;
import com.itau.srv.motor.compras.mapper.EventoIRMapper;
import com.itau.srv.motor.compras.model.enums.TipoIR;
import com.itau.srv.motor.compras.repository.EventoIRRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventoIRService {

    private final IREventProducer irEventProducer;
    private final EventoIRRepository eventoIRRepository;
    private final EventoIRMapper eventoIRMapper;

    @Transactional
    public void publicarEventoIR(ClienteAgrupamentoDTO cliente, String ticker, int quantidade, BigDecimal precoUnitario) {
        log.info("   Publicando evento IR para cliente {} - {} ({} ações)",
                cliente.clienteId(), ticker, quantidade);

        IRDedoDuroEventDTO evento = criarParaCompra(cliente.clienteId(), cliente.cpf(), ticker, quantidade, precoUnitario);

        irEventProducer.publicarEventoIRDedoDuro(evento);

        eventoIRRepository.save(eventoIRMapper.mapearParaEventoIr(evento));

        log.info("   >>> IR calculado: R$ {} (0,005% sobre R$ {})",
                evento.valorIR(), evento.valorOperacao());
    }

    public static IRDedoDuroEventDTO criarParaCompra(
            Long clienteId,
            String cpf,
            String ticker,
            Integer quantidade,
            BigDecimal precoUnitario) {

        BigDecimal valorOperacao = precoUnitario.multiply(BigDecimal.valueOf(quantidade));

        BigDecimal aliquota = new BigDecimal("0.00005");

        BigDecimal valorIR = valorOperacao.multiply(aliquota)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        return new IRDedoDuroEventDTO(
                TipoIR.IR_DEDO_DURO.toString(),
                clienteId,
                cpf,
                ticker,
                "COMPRA",
                quantidade,
                precoUnitario,
                valorOperacao,
                aliquota,
                valorIR,
                LocalDateTime.now()
        );
    }
}
