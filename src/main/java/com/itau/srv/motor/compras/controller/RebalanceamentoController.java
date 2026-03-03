package com.itau.srv.motor.compras.controller;

import com.itau.srv.motor.compras.dto.ir.RebalancementoEventDTO;
import com.itau.srv.motor.compras.kafka.RebalanceamentoEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rebalanceamentos")
@RequiredArgsConstructor
@Slf4j
public class RebalanceamentoController {

    private final RebalanceamentoEventProducer rebalanceamentoEventProducer;

    @PostMapping("/eventos")
    public ResponseEntity<Void> postarRebalanceamentoKafka(@RequestBody RebalancementoEventDTO evento) {
        log.info("Rebalanceamento: {}", evento);

        rebalanceamentoEventProducer.publicarEventoRebalanceamento(evento);

        return ResponseEntity.noContent().build();
    }
}
