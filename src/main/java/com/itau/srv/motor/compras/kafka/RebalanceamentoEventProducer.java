package com.itau.srv.motor.compras.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itau.srv.motor.compras.dto.ir.RebalancementoEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RebalanceamentoEventProducer {

    private static final String TOPIC_REBALANCEAMENTO = "rebalanceamento-cesta";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publicarEventoRebalanceamento(RebalancementoEventDTO evento) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            String eventoJson = objectMapper.writeValueAsString(evento);

            kafkaTemplate.send(TOPIC_REBALANCEAMENTO, evento.cestaAnteriorId().toString(), eventoJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("✓ Evento IR publicado: Cesta Antiga {} - Cesta Atual {} - Data Rebalanceamento {}",
                                    evento.cestaAnteriorId(), evento.cestaAtualId(), evento.dataExecucao());
                        } else {
                            log.error("❌ ERRO ao publicar evento IR: {}", ex.getMessage());
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("❌ ERRO ao serializar evento IR: {}", e.getMessage());
        }
    }
}
