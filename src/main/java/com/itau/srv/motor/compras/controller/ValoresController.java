package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.historico.HistoricoAportesResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresPorDataResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresResponseDTO;
import com.itau.srv.motor.compras.service.ValoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/valores")
@RequiredArgsConstructor
public class ValoresController {

    private final ValoresService valoresService;

    @GetMapping("/{clienteId}")
    public ResponseEntity<ValoresResponseDTO> obterValoresPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(valoresService.calcularValoresCliente(clienteId));
    }

    @GetMapping
    public ResponseEntity<ValoresPorDataResponseDTO> obterValorPorClienteEData(
            @RequestParam(name = "clienteId") Long clienteId,
            @RequestParam(name = "data") LocalDate data
    ) {
        return ResponseEntity.ok(valoresService.consultarPorClienteEData(clienteId, data));
    }

    @GetMapping("/historico/{clienteId}")
    public ResponseEntity<List<HistoricoAportesResponseDTO>> consultarHistoricoAportes(@PathVariable Long clienteId) {
        return ResponseEntity.ok(valoresService.consultarHistoricoAportes(clienteId));
    }
}
