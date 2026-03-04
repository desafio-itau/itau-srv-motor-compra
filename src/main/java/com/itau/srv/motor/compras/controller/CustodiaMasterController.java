package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.custodia.CustodiaMasterResponseDTO;
import com.itau.srv.motor.compras.service.CustodiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Custódia Master", description = "Endpoints para consulta da custódia master (resíduos de distribuição)")
@RestController
@RequestMapping("/api/custodia-master")
@RequiredArgsConstructor
@Slf4j
public class CustodiaMasterController {

    private final CustodiaService custodiaService;

    @Operation(
            summary = "Obter custódia master",
            description = "Retorna a custódia da conta master contendo os resíduos de ações não distribuídas aos clientes"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Custódia master retornada com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustodiaMasterResponseDTO.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<CustodiaMasterResponseDTO> obterCustodiaMaster() {
        return ResponseEntity.ok(custodiaService.consultarCustodiaMaster());
    }
}
