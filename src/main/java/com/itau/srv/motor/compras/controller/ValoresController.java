package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.historico.HistoricoAportesResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresPorDataResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresResponseDTO;
import com.itau.srv.motor.compras.service.ValoresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Valores", description = "Endpoints para consulta de valores investidos e histórico de aportes")
@RestController
@RequestMapping("/api/valores")
@RequiredArgsConstructor
public class ValoresController {

    private final ValoresService valoresService;

    @Operation(
            summary = "Obter valores por cliente",
            description = "Retorna o total investido e vendido por um cliente específico"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Valores retornados com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValoresResponseDTO.class)
                    )
            )
    })
    @GetMapping("/{clienteId}")
    public ResponseEntity<ValoresResponseDTO> obterValoresPorCliente(
            @Parameter(description = "ID do cliente", required = true, example = "1")
            @PathVariable Long clienteId
    ) {
        return ResponseEntity.ok(valoresService.calcularValoresCliente(clienteId));
    }

    @Operation(
            summary = "Obter valores por cliente e data",
            description = "Retorna o total investido e vendido por um cliente em uma data específica"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Valores retornados com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValoresPorDataResponseDTO.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<ValoresPorDataResponseDTO> obterValorPorClienteEData(
            @Parameter(description = "ID do cliente", required = true, example = "1")
            @RequestParam(name = "clienteId") Long clienteId,
            @Parameter(description = "Data de referência", required = true, example = "2026-03-01")
            @RequestParam(name = "data") LocalDate data
    ) {
        return ResponseEntity.ok(valoresService.consultarPorClienteEData(clienteId, data));
    }

    @Operation(
            summary = "Consultar histórico de aportes",
            description = "Retorna o histórico de aportes realizados por um cliente, agrupados por data de execução"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Histórico retornado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = HistoricoAportesResponseDTO.class))
                    )
            )
    })
    @GetMapping("/historico/{clienteId}")
    public ResponseEntity<List<HistoricoAportesResponseDTO>> consultarHistoricoAportes(
            @Parameter(description = "ID do cliente", required = true, example = "1")
            @PathVariable Long clienteId
    ) {
        return ResponseEntity.ok(valoresService.consultarHistoricoAportes(clienteId));
    }
}
