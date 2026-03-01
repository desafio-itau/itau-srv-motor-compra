package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraRequestDTO;
import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraResponseDTO;
import com.itau.srv.motor.compras.service.MotorCompraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/motor/executar-compra")
@RequiredArgsConstructor
@Slf4j
public class MotorCompraController {

    private final MotorCompraService motorCompraService;

    @PostMapping
    public ResponseEntity<MotorCompraResponseDTO> executarCompra(@RequestBody MotorCompraRequestDTO request) {
        return ResponseEntity.ok(motorCompraService.acionarMotorCompra(request));
    }
}
