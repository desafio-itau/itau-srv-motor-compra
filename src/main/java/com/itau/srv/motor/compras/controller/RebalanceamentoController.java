package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.ir.RebalancementoEventDTO;
import com.itau.srv.motor.compras.kafka.RebalanceamentoEventProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rebalanceamento", description = "Endpoints para publicação de eventos de rebalanceamento de cesta")
@RestController
@RequestMapping("/api/rebalanceamentos")
@RequiredArgsConstructor
@Slf4j
public class RebalanceamentoController {

    private final RebalanceamentoEventProducer rebalanceamentoEventProducer;

    @Operation(
            summary = "Publicar evento de rebalanceamento",
            description = "Publica um evento no Kafka para iniciar o processo de rebalanceamento de cesta. " +
                    "O consumidor irá processar vendas de ativos que saíram, compras de ativos que entraram " +
                    "e rebalanceamento dos ativos que permaneceram na cesta."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Evento publicado com sucesso no Kafka",
                    content = @Content
            ),
            @ApiResponse(responseCode = "400", description = "Erro de validação: dados do evento inválidos", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro ao publicar evento no Kafka", content = @Content)
    })
    @PostMapping("/eventos")
    public ResponseEntity<Void> postarRebalanceamentoKafka(
            @RequestBody(description = "Dados do evento de rebalanceamento", required = true)
            @org.springframework.web.bind.annotation.RequestBody RebalancementoEventDTO evento
    ) {
        log.info("Rebalanceamento: {}", evento);

        rebalanceamentoEventProducer.publicarEventoRebalanceamento(evento);

        return ResponseEntity.noContent().build();
    }
}
