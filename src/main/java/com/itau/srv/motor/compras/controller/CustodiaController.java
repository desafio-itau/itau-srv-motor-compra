package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.custodia.CustodiaResponseDTO;
import com.itau.srv.motor.compras.service.CustodiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Custódia", description = "Endpoints para consulta de custódia de clientes")
@RestController
@RequestMapping("/api/custodias")
@RequiredArgsConstructor
@Slf4j
public class CustodiaController {

    private final CustodiaService custodiaService;

    @Operation(
            summary = "Buscar custódia do cliente",
            description = "Retorna todas as custódias (posição de ativos) de um cliente específico com quantidade maior que zero"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Custódias encontradas com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CustodiaResponseDTO.class))
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content)
    })
    @GetMapping("/{clienteId}")
    public ResponseEntity<List<CustodiaResponseDTO>> buscarCustodiasCliente(
            @Parameter(description = "ID do cliente", required = true, example = "1")
            @PathVariable Long clienteId
    ) {
        return ResponseEntity.ok(custodiaService.consultarCustodiasCliente(clienteId));
    }
}
