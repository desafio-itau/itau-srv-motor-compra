package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.custodia.CustodiaResponseDTO;
import com.itau.srv.motor.compras.service.CustodiaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/custodias")
@RequiredArgsConstructor
@Slf4j
public class CustodiaController {

    private final CustodiaService custodiaService;

    @GetMapping("/{clienteId}")
    public ResponseEntity<List<CustodiaResponseDTO>> buscarCustodiasCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(custodiaService.consultarCustodiasCliente(clienteId));
    }
}
