package com.itau.srv.motor.compras.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itau.srv.motor.compras.dto.ir.IRDedoDuroEventDTO;
import com.itau.srv.motor.compras.dto.ir.RebalancementoEventDTO;
import com.itau.srv.motor.compras.dto.rebalanceamento.IRVendaMensalEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IREventProducer {

    private static final String TOPIC_IR_DEDO_DURO = "ir-dedo-duro";
    private static final String TOPIC_IR_VENDAS = "ir-vendas";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publicarEventoIRDedoDuro(IRDedoDuroEventDTO evento) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            String eventoJson = objectMapper.writeValueAsString(evento);

            kafkaTemplate.send(TOPIC_IR_DEDO_DURO, evento.cpf(), eventoJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("✓ Evento IR publicado: Cliente {} - {} - R$ {}",
                                    evento.clienteId(), evento.ticker(), evento.valorIR());
                        } else {
                            log.error("❌ ERRO ao publicar evento IR: {}", ex.getMessage());
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("❌ ERRO ao serializar evento IR: {}", e.getMessage());
        }
    }

    public void publicarEventoIRVendas(IRVendaMensalEventDTO evento) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            String eventoJson = objectMapper.writeValueAsString(evento);

            kafkaTemplate.send(TOPIC_IR_VENDAS, evento.cpf(), eventoJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("✓ Evento IR VENDAS publicado: Cliente {} - Mês {} - IR: R$ {}",
                                    evento.clienteId(), evento.mesReferencia(), evento.valorIR());
                        } else {
                            log.error("❌ ERRO ao publicar evento IR VENDAS: {}", ex.getMessage());
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("❌ ERRO ao serializar evento IR VENDAS: {}", e.getMessage());
        }
    }
}
