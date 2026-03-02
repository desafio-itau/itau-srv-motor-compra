package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.valor.ValoresResponseDTO;
import com.itau.srv.motor.compras.model.EventoIR;
import com.itau.srv.motor.compras.repository.EventoIRRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValoresService {

    private final EventoIRRepository eventoIRRepository;

    @Transactional(readOnly = true)
    public ValoresResponseDTO calcularValoresCliente(Long clienteId) {
        BigDecimal valorInvestido = BigDecimal.ZERO;
        BigDecimal valorVendido = BigDecimal.ZERO;

        log.info("Calculando valor investido por cliente: {}", clienteId);
        List<EventoIR> investidosPorCliente = eventoIRRepository.findInvestidoPorCliente(clienteId);
        List<EventoIR> vendidoPorCliente = eventoIRRepository.findVendidoPorCliente(clienteId);

        if (investidosPorCliente.isEmpty()) {
            log.info("Nenhum evento IR encontrado para o cliente: {} / Valor investido: R$ {}", clienteId, valorInvestido);
            return new ValoresResponseDTO(
                    valorInvestido,
                    valorVendido
            );
        }

        for (EventoIR evento : investidosPorCliente) {
            valorInvestido = valorInvestido.add(evento.getValorBase());
        }

        for (EventoIR evento : vendidoPorCliente) {
            valorVendido = valorVendido.add(evento.getValorBase());
        }

        log.info("Valor investido por cliente: {} / Valor: R$ {}", clienteId, valorInvestido);
        log.info("Valor vendido por cliente: {} / Valor: R$ {}", clienteId, valorVendido);

        return new ValoresResponseDTO(
                valorInvestido,
                valorVendido
        );
    }
}
