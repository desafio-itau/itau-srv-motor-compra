package com.itau.srv.motor.compras.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itau.srv.motor.compras.dto.ir.RebalancementoEventDTO;
import com.itau.srv.motor.compras.service.RebalanceamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RebalanceamentoEventConsumer {

    private final RebalanceamentoService rebalanceamentoService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "rebalanceamento-cesta",
            groupId = "motor-compras-rebalanceamento-group"
    )
    public void consumirEventoRebalanceamento(String mensagem) {
        try {
            log.info("📥 Mensagem recebida no tópico rebalanceamento-cesta");

            objectMapper.registerModule(new JavaTimeModule());
            RebalancementoEventDTO evento = objectMapper.readValue(mensagem, RebalancementoEventDTO.class);

            log.info("✓ Evento deserializado com sucesso");
            log.info("  - Cesta Anterior ID: {}", evento.cestaAnteriorId());
            log.info("  - Cesta Atual ID: {}", evento.cestaAtualId());
            log.info("  - Data Execução: {}", evento.dataExecucao());

            LocalDate dataExecucao = evento.dataExecucao().toLocalDate();
            rebalanceamentoService.executarRebalanceamento(
                    evento.cestaAnteriorId(),
                    evento.cestaAtualId(),
                    dataExecucao
            );

            log.info("✅ Rebalanceamento processado com sucesso!");

        } catch (Exception e) {
            log.error("❌ ERRO ao processar evento de rebalanceamento: {}", e.getMessage(), e);
            // TODO: Implementar retry logic ou dead letter queue
        }
    }
}

