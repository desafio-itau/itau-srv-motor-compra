package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.ir.EventoIRSumarioDTO;
import com.itau.srv.motor.compras.dto.historico.HistoricoAportesResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresPorDataResponseDTO;
import com.itau.srv.motor.compras.dto.valor.ValoresResponseDTO;
import com.itau.srv.motor.compras.mapper.HistoricoMapper;
import com.itau.srv.motor.compras.model.EventoIR;
import com.itau.srv.motor.compras.repository.EventoIRRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValoresService {

    private final EventoIRRepository eventoIRRepository;
    private final HistoricoMapper historicoMapper;

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

    @Transactional(readOnly = true)
    public List<HistoricoAportesResponseDTO> consultarHistoricoAportes(Long clienteId) {
        log.info("Consultando histórico de aportes por cliente: {}", clienteId);

        List<HistoricoAportesResponseDTO> historicos = new ArrayList<>();

        List<EventoIRSumarioDTO> sumarios = eventoIRRepository.findSumarioEventosPorDataECliente(clienteId);

        log.info(String.valueOf(sumarios.size()));

        for (EventoIRSumarioDTO sumario : sumarios) {
            HistoricoAportesResponseDTO historico = historicoMapper.mapearParaHistoricoAportes(sumario);
            historicos.add(historico);
        }

        log.info("Quantidade de históricos encontrados para cliente {}: {}", clienteId, historicos.size());

        return historicos;
    }
}
