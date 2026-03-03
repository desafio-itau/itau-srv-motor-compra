package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.valor.ValoresPorDataResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresResponseDTO;
import com.itau.srv.motor.compras.model.EventoIR;
import com.itau.srv.motor.compras.repository.EventoIRRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Transactional(readOnly = true)
    public ValoresPorDataResponseDTO consultarPorClienteEData(Long clienteId, LocalDate data) {
        BigDecimal valorInvestido = BigDecimal.ZERO;
        BigDecimal valorVendido = BigDecimal.ZERO;

        log.info("Calculando valor investido por cliente: {} e data: {}", clienteId, data);
        List<EventoIR> investidosPorClienteEData = eventoIRRepository.findInvestidoPorClienteEData(clienteId, data);
        List<EventoIR> vendidoPorClienteEData = eventoIRRepository.findVendidoPorClienteEData(clienteId, data);

        for (EventoIR evento : investidosPorClienteEData) {
            valorInvestido = valorInvestido.add(evento.getValorBase());
        }

        for (EventoIR evento : vendidoPorClienteEData) {
            valorVendido = valorVendido.add(evento.getValorBase());
        }

        log.info("Valor investido por cliente: {} e data: {} / Valor: R$ {}", clienteId, data, valorInvestido);
        log.info("Valor vendido por cliente: {} e data: {} / Valor: R$ {}", clienteId, data, valorVendido);

        return new ValoresPorDataResponseDTO(
                data,
                new ValoresResponseDTO(
                        valorInvestido,
                        valorVendido
                )
        );
    }
}
