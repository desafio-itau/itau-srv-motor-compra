package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.custodia.CustodiaMasterResponseDTO;
import com.itau.srv.motor.compras.service.CustodiaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/custodia-master")
@RequiredArgsConstructor
@Slf4j
public class CustodiaMasterController {

    private final CustodiaService custodiaService;

    @GetMapping
    public ResponseEntity<CustodiaMasterResponseDTO> obterCustodiaMaster() {
        return ResponseEntity.ok(custodiaService.consultarCustodiaMaster());
    }
}
