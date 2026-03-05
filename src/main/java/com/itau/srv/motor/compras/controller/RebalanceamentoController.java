package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.ir.RebalancementoEventDTO;
import com.itau.srv.motor.compras.dto.rebalanceamento.RebalanceamentoDesvioRequestDTO;
import com.itau.srv.motor.compras.dto.rebalanceamento.RebalanceamentoDesvioResponseDTO;
import com.itau.srv.motor.compras.kafka.RebalanceamentoEventProducer;
import com.itau.srv.motor.compras.service.RebalanceamentoService;
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
    private final RebalanceamentoService rebalanceamentoService;

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

    @Operation(
            summary = "Executar rebalanceamento por desvio de proporção",
            description = "Analisa todas as carteiras dos clientes ativos e realiza rebalanceamento " +
                    "quando a proporção atual de um ativo diverge significativamente do percentual " +
                    "da cesta recomendada (limiar padrão: 5 pontos percentuais). " +
                    "Vende ativos sobre-alocados e compra ativos sub-alocados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rebalanceamento por desvio executado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RebalanceamentoDesvioResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Erro de validação: limiar ou data inválidos", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro ao executar rebalanceamento", content = @Content)
    })
    @PostMapping("/desvio")
    public ResponseEntity<RebalanceamentoDesvioResponseDTO> executarRebalanceamentoPorDesvio(
            @RequestBody(description = "Dados do rebalanceamento por desvio", required = true)
            @org.springframework.web.bind.annotation.RequestBody RebalanceamentoDesvioRequestDTO request
    ) {
        log.info("Iniciando rebalanceamento por desvio - Limiar: {}%, Data: {}",
                request.limiarDesvio(), request.dataExecucao());

        RebalanceamentoDesvioResponseDTO response = rebalanceamentoService
                .executarRebalanceamentoPorDesvio(request.limiarDesvio(), request.dataExecucao());

        log.info("Rebalanceamento por desvio finalizado - Clientes rebalanceados: {}/{}",
                response.totalClientesRebalanceados(), response.totalClientesProcessados());

        return ResponseEntity.ok(response);
    }
}
