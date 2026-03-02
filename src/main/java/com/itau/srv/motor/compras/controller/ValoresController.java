package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.valor.ValoresResponseDTO;
import com.itau.srv.motor.compras.service.ValoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/valores")
@RequiredArgsConstructor
public class ValoresController {

    private final ValoresService valoresService;

    @GetMapping("/{clienteId}")
    public ResponseEntity<ValoresResponseDTO> obterValoresPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(valoresService.calcularValoresCliente(clienteId));
    }
}
