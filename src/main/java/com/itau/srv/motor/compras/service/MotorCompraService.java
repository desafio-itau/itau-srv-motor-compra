package com.itau.srv.motor.compras.service;

import com.itau.common.library.exception.ConflitoException;
import com.itau.srv.motor.compras.dto.agrupamento.AgrupamentoResponseDTO;
import com.itau.srv.motor.compras.dto.distribuicao.DistribuicaoResponseDTO;
import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraRequestDTO;
import com.itau.srv.motor.compras.dto.motorcompra.MotorCompraResponseDTO;
import com.itau.srv.motor.compras.dto.ordemcompra.CalcularQuantidadeAtivoResponse;
import com.itau.srv.motor.compras.dto.residuo.ResiduoContaMasterDTO;
import com.itau.srv.motor.compras.feign.ClientesFeignClient;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import com.itau.srv.motor.compras.repository.OrdemCompraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MotorCompraService {

    private final CustodiaRepository custodiaRepository;
    private final CustodiaService custodiaService;
    private final QuantidadeAtivoService quantidadeAtivoService;
    private final ClienteService clienteService;
    private final OrdemCompraRepository ordemCompraRepository;
    private final ClientesFeignClient clientesFeignClient;

    @Transactional
    public MotorCompraResponseDTO acionarMotorCompra(MotorCompraRequestDTO request) {
        if (ordemCompraRepository.existsByDataExecucao(request.dataReferencia())) {
            log.error("Uma compra já foi executada nessa data");
            throw new ConflitoException("COMPRA_JA_EXECUTADA");
        }

        validarDiaUtil(request.dataReferencia());

        AgrupamentoResponseDTO agrupamentoClientes = clienteService.agruparClientes();

        CalcularQuantidadeAtivoResponse resultadoCalculo = quantidadeAtivoService.calcularQuantidadePorAtivo(agrupamentoClientes.valorConsolidado(), request.dataReferencia());

        List<DistribuicaoResponseDTO> distribuicaoCustodias = custodiaService.distribuirCustodias(resultadoCalculo, agrupamentoClientes);

        List<Custodia> residuos = custodiaRepository.findCustodiaMaster();

        List<ResiduoContaMasterDTO> residuosContaMaster = new ArrayList<>();

        if (residuos != null && !residuos.isEmpty()) {
            for (Custodia residuo : residuos) {
                ResiduoContaMasterDTO residuoDTO = new ResiduoContaMasterDTO(
                        residuo.getTicker(),
                        residuo.getQuantidade()
                );
                residuosContaMaster.add(residuoDTO);
            }
        }

        int totalEventosIR = 0;
        for (DistribuicaoResponseDTO dist : distribuicaoCustodias) {
            totalEventosIR += dist.ativos().size();
        }

        clientesFeignClient.criarSnapshots(request.dataReferencia());

        return new MotorCompraResponseDTO(
                request.dataReferencia().atTime(LocalTime.now()),
                agrupamentoClientes.agrupamentoCliente().size(),
                agrupamentoClientes.valorConsolidado(),
                resultadoCalculo.dtos(),
                distribuicaoCustodias,
                residuosContaMaster,
                totalEventosIR,
                "Compra programada executada com sucesso para " + agrupamentoClientes.agrupamentoCliente().size() + " clientes."
        );
    }

    private void validarDiaUtil(LocalDate dataReferencia) {
        DayOfWeek diaSemana = dataReferencia.getDayOfWeek();

        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            log.warn("Tentativa de executar motor de compras em dia não útil: {} ({})",
                    dataReferencia, diaSemana);

            throw  new RuntimeException(
                    "Nosso sistema de motor de compras só funciona em dias úteis. " +
                            "Fique tranquilo, pois sua ordem de compra será executada no próximo dia útil."
            );
        }

        log.info("Data validada como dia útil: {} ({})", dataReferencia, diaSemana);
    }
}
