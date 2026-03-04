package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraRequestDTO;
import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraResponseDTO;
import com.itau.srv.motor.compras.service.MotorCompraService;
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

@Tag(name = "Motor de Compra", description = "Endpoint para execução do motor de compra programada")
@RestController
@RequestMapping("/api/motor/executar-compra")
@RequiredArgsConstructor
@Slf4j
public class MotorCompraController {

    private final MotorCompraService motorCompraService;

    @Operation(
            summary = "Executar motor de compra",
            description = "Aciona o motor de compra programada que realiza: agrupamento de clientes ativos, " +
                    "cálculo de quantidade por ativo, separação em lote padrão e fracionário, " +
                    "distribuição proporcional aos clientes e atualização de resíduos na conta master. " +
                    "Execução permitida apenas em dias úteis (segunda a sexta-feira)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Motor de compra executado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MotorCompraResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Erro de validação: data de referência inválida ou dia não útil", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflito: já existe uma compra executada para esta data", content = @Content)
    })
    @PostMapping
    public ResponseEntity<MotorCompraResponseDTO> executarCompra(
            @RequestBody(description = "Dados da execução do motor de compra", required = true)
            @org.springframework.web.bind.annotation.RequestBody MotorCompraRequestDTO request
    ) {
        return ResponseEntity.ok(motorCompraService.acionarMotorCompra(request));
    }
}
