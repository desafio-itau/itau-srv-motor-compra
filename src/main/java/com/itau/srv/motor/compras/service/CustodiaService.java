package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.agrupamento.AgrupamentoResponseDTO;
import com.itau.srv.motor.compras.dto.agrupamento.ClienteAgrupamentoDTO;
import com.itau.srv.motor.compras.dto.ativo.AtivoResponseDTO;
import com.itau.srv.motor.compras.dto.conta.ContaGraficaDTO;
import com.itau.srv.motor.compras.dto.cotacao.CotacaoResponseDTO;
import com.itau.srv.motor.compras.dto.custodia.CustodiaMasterResponseDTO;
import com.itau.srv.motor.compras.dto.custodia.CustodiaResponseDTO;
import com.itau.srv.motor.compras.dto.distribuicao.DistribuicaoResponseDTO;
import com.itau.srv.motor.compras.dto.ordemcompra.CalcularQuantidadeAtivoResponse;
import com.itau.srv.motor.compras.feign.ContasGraficasFeignClient;
import com.itau.srv.motor.compras.feign.CotacaoFeignClient;
import com.itau.srv.motor.compras.mapper.CustodiaMapper;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.model.OrdemCompra;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustodiaService {

    private final CustodiaRepository custodiaRepository;
    private final EventoIRService eventoIRService;
    private final ResiduoService residuoService;
    private final ContasGraficasFeignClient contasGraficasFeignClient;
    private final CotacaoFeignClient cotacaoFeignClient;
    private final CustodiaMapper custodiaMapper;

    @Value("${motor.compras.conta-master-id}")
    private Long contaMasterId;

    @Transactional
    public List<DistribuicaoResponseDTO> distribuirCustodias(CalcularQuantidadeAtivoResponse resultadoCalculoAtivos, AgrupamentoResponseDTO agrupamento, LocalDate dataReferencia) {
        log.info("Total de clientes: {}", agrupamento.agrupamentoCliente().size());
        log.info("Valor consolidado: R$ {}", agrupamento.valorConsolidado());

        List<Custodia> custodiasMaster = custodiaRepository.findCustodiaMaster();
        log.info("Custodias master encontradas: {}", custodiasMaster.size());

        Map<Long, List<AtivoResponseDTO>> distribuicoesPorCliente = new HashMap<>();

        List<OrdemCompra> entities = resultadoCalculoAtivos.entities();

        for (OrdemCompra ordemCompra : entities) {
            log.info("--- Distribuindo ativo: {} ---", ordemCompra.getTicker());

            int saldoMaster = 0;
            for (Custodia custodiaMaster : custodiasMaster) {
                if (custodiaMaster.getTicker().equals(ordemCompra.getTicker())) {
                    saldoMaster = custodiaMaster.getQuantidade();
                    log.info("Saldo master encontrado para {}: {} ações", ordemCompra.getTicker(), saldoMaster);
                    break;
                }
            }

            int totalDisponivel = ordemCompra.getQuantidade() + saldoMaster;
            log.info("Total disponível para distribuir de {}: {} ações (compradas: {} + saldo master: {})",
                    ordemCompra.getTicker(), totalDisponivel, ordemCompra.getQuantidade(), saldoMaster);

            int totalDistribuido = 0;

            for (ClienteAgrupamentoDTO cliente : agrupamento.agrupamentoCliente()) {
                log.info(">> Processando cliente: {} (ID: {})", cliente.nome(), cliente.clienteId());
                log.info("   Aporte do cliente: R$ {}", cliente.valorAportado());

                BigDecimal proporcao = cliente.valorAportado()
                        .divide(agrupamento.valorConsolidado(), 10, RoundingMode.DOWN);
                log.info("   Proporção do cliente: {} ({} %)", proporcao, proporcao.multiply(BigDecimal.valueOf(100)));

                int quantidadeParaCliente = proporcao
                        .multiply(BigDecimal.valueOf(totalDisponivel))
                        .setScale(0, RoundingMode.DOWN)
                        .intValue();
                log.info("   Quantidade para cliente: {} ações (TRUNCAR({} × {}))",
                        quantidadeParaCliente, proporcao, totalDisponivel);

                totalDistribuido += quantidadeParaCliente;

                if (quantidadeParaCliente == 0) {
                    log.warn("   Quantidade zero para cliente {}. Pulando.", cliente.clienteId());
                    continue;
                }

                Custodia custodiaExistente = null;

                List<Custodia> custodiasCliente = custodiaRepository.findCustodiaCliente(cliente.clienteId());

                for (Custodia custodia : custodiasCliente) {
                    if (custodia.getTicker().equals(ordemCompra.getTicker())) {
                        custodiaExistente = custodia;
                        break;
                    }
                }

                if (custodiaExistente == null) {
                    log.info("   Cliente não possui {}. Criando nova custodia.", ordemCompra.getTicker());

                    BigDecimal precoMedio = ordemCompra.getPrecoUnitario();

                    Custodia novaCustodia = new Custodia();
                    novaCustodia.setTicker(ordemCompra.getTicker());
                    novaCustodia.setQuantidade(quantidadeParaCliente);
                    novaCustodia.setContaGraficaId(cliente.contaId());
                    novaCustodia.setPrecoMedio(precoMedio);

                    custodiaRepository.save(novaCustodia);
                    log.info("   Custodia criada: {} ações de {} com PM = R$ {}",
                            quantidadeParaCliente, ordemCompra.getTicker(), precoMedio);

                    eventoIRService.publicarEventoIR(cliente, ordemCompra.getTicker(), quantidadeParaCliente, ordemCompra.getPrecoUnitario(), dataReferencia);

                } else {
                    log.info("   Cliente já possui {}. Atualizando custodia.", ordemCompra.getTicker());
                    log.info("   Posição anterior: {} ações com PM = R$ {}",
                            custodiaExistente.getQuantidade(), custodiaExistente.getPrecoMedio());

                    int qtdAnterior = custodiaExistente.getQuantidade();
                    BigDecimal pmAnterior = custodiaExistente.getPrecoMedio();
                    int qtdNova = quantidadeParaCliente;
                    BigDecimal precoNova = ordemCompra.getPrecoUnitario();

                    BigDecimal numerador = pmAnterior.multiply(BigDecimal.valueOf(qtdAnterior))
                            .add(precoNova.multiply(BigDecimal.valueOf(qtdNova)));
                    BigDecimal denominador = BigDecimal.valueOf(qtdAnterior + qtdNova);
                    BigDecimal novoPrecoMedio = numerador.divide(denominador, 2, RoundingMode.HALF_UP);

                    log.info("   Cálculo PM: ({} × {} + {} × {}) / ({} + {}) = R$ {}",
                            qtdAnterior, pmAnterior, qtdNova, precoNova,
                            qtdAnterior, qtdNova, novoPrecoMedio);

                    custodiaExistente.setQuantidade(qtdAnterior + qtdNova);
                    custodiaExistente.setPrecoMedio(novoPrecoMedio);

                    custodiaRepository.save(custodiaExistente);
                    log.info("   Custodia atualizada: {} ações de {} com PM = R$ {}",
                            custodiaExistente.getQuantidade(), ordemCompra.getTicker(), novoPrecoMedio);

                    eventoIRService.publicarEventoIR(cliente, ordemCompra.getTicker(), quantidadeParaCliente, ordemCompra.getPrecoUnitario(), dataReferencia);
                }

                distribuicoesPorCliente.computeIfAbsent(cliente.clienteId(), k -> new ArrayList<>())
                        .add(new AtivoResponseDTO(ordemCompra.getTicker(), quantidadeParaCliente));

                log.info("   ✓ Distribuído {} ações de {} para cliente {}",
                        quantidadeParaCliente, ordemCompra.getTicker(), cliente.nome());
            }

            int residuo = totalDisponivel - totalDistribuido;
            log.info("Total distribuído de {}: {} ações", ordemCompra.getTicker(), totalDistribuido);
            log.info("Resíduo de {} para custodia master: {} ações", ordemCompra.getTicker(), residuo);

            residuoService.atualizarResiduosCustodiaMaster(ordemCompra.getTicker(), residuo, ordemCompra.getPrecoUnitario(), custodiasMaster);
        }

        List<DistribuicaoResponseDTO> distribuicoes = new ArrayList<>();

        for (ClienteAgrupamentoDTO cliente : agrupamento.agrupamentoCliente()) {
            List<AtivoResponseDTO> ativosDistribuidos = distribuicoesPorCliente.get(cliente.clienteId());

            if (ativosDistribuidos != null && !ativosDistribuidos.isEmpty()) {
                DistribuicaoResponseDTO distribuicao = new DistribuicaoResponseDTO(
                        cliente.clienteId(),
                        cliente.nome(),
                        cliente.valorAportado(),
                        ativosDistribuidos
                );
                distribuicoes.add(distribuicao);
            }
        }

        log.info("=== Distribuição concluída para todos os {} cliente(s) ===", distribuicoes.size());
        return distribuicoes;
    }

    @Transactional(readOnly = true)
    public CustodiaMasterResponseDTO consultarCustodiaMaster() {
        log.info("Consultando custodia master");

        BigDecimal valorTotalResiduo = BigDecimal.ZERO;
        List<CustodiaResponseDTO> custodias = new ArrayList<>();

        ContaGraficaDTO contaMaster = contasGraficasFeignClient.buscarConta(contaMasterId);
        log.info("Conta master encontrada: ID {}, Tipo {}", contaMaster.id(), contaMaster.tipoConta());

        log.info("Buscando custodias master");
        List<Custodia> custodiasMaster = custodiaRepository.findCustodiaMaster();
        log.info("Custodias master encontradas: {}", custodiasMaster.size());

        for (Custodia custodia : custodiasMaster) {
            if (custodia.getQuantidade() > 0) {
                CotacaoResponseDTO cotacao = cotacaoFeignClient.obterCotacaoPorTicker(custodia.getTicker());
                valorTotalResiduo = valorTotalResiduo.add(cotacao.precoFechamento().multiply(BigDecimal.valueOf(custodia.getQuantidade())));

                custodias.add(custodiaMapper.mapearParaCustodiaResponse(custodia, cotacao.precoFechamento()));
            }
        }

        log.info("Custodias master ativas (quantidade > 0): {}", custodias.size());

        return custodiaMapper.mapearParaCustodiaMasterResponseDTO(contaMaster, custodias, valorTotalResiduo);
    }

    @Transactional(readOnly = true)
    public List<CustodiaResponseDTO> consultarCustodiasCliente(Long clienteId) {
        log.info("Consultando custodia para cliente: {}", clienteId);
        List<CustodiaResponseDTO> custodiasResponse = new ArrayList<>();

        List<Custodia> custodiasCliente = custodiaRepository.findCustodiaCliente(clienteId);
        log.info("Custodias encontradas: {}", custodiasCliente.size());

        // Filtrar apenas custodias com quantidade > 0
        for (Custodia custodia : custodiasCliente) {
            if (custodia.getQuantidade() > 0) {
                CotacaoResponseDTO cotacao = cotacaoFeignClient.obterCotacaoPorTicker(custodia.getTicker());
                custodiasResponse.add(custodiaMapper.mapearParaCustodiaResponse(custodia, cotacao.precoFechamento()));
            }
        }

        log.info("Custodias ativas (quantidade > 0): {}", custodiasResponse.size());
        return custodiasResponse;
    }
}
