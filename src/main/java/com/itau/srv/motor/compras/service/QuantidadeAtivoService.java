package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.cesta.CestaResponseDTO;
import com.itau.srv.motor.compras.dto.ordemcompra.CalcularQuantidadeAtivoResponse;
import com.itau.srv.motor.compras.dto.ordemcompra.DetalhesOrdemCompra;
import com.itau.srv.motor.compras.dto.ordemcompra.OrdemCompraResponseDTO;
import com.itau.srv.motor.compras.feign.CestaFeignClient;
import com.itau.srv.motor.compras.mapper.OrdemCompraMapper;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.model.OrdemCompra;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import com.itau.srv.motor.compras.repository.OrdemCompraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuantidadeAtivoService {

    private final CestaFeignClient cestaFeignClient;
    private final CustodiaRepository custodiaRepository;
    private final OrdemCompraMapper ordemCompraMapper;
    private final OrdemCompraRepository ordemCompraRepository;

    @Value("${motor.compras.conta-master-id}")
    private Long contaMasterId;

    @Transactional
    public CalcularQuantidadeAtivoResponse calcularQuantidadePorAtivo(BigDecimal valorConsolidado, LocalDate dataReferencia) {
        log.info("Valor consolidado recebido: R$ {}", valorConsolidado);

        log.info("Buscando cesta ativa");
        CestaResponseDTO cestaAtiva = cestaFeignClient.obterCestaAtiva();
        log.info("Cesta ativa encontrada: {}", cestaAtiva.nome());

        log.info("Buscando custodias master (resíduos)");
        List<Custodia> custodiasMaster = custodiaRepository.findCustodiaMaster();
        log.info("Custodias master encontradas: {}", custodiasMaster.size());

        List<OrdemCompra> entities = new ArrayList<>();
        List<OrdemCompraResponseDTO> dtos = new ArrayList<>();

        for (var item : cestaAtiva.itens()) {
            log.info("--- Processando ativo: {} ({}%) ---", item.ticker(), item.percentual());

            // ✅ CORRIGIDO: Dividir percentual por 100 (vem como 20, precisa ser 0.20)
            BigDecimal percentualDecimal = item.percentual().divide(BigDecimal.valueOf(100), 10, RoundingMode.DOWN);
            BigDecimal valorPorAtivo = valorConsolidado.multiply(percentualDecimal);
            log.info("Valor para {}: R$ {} ({}% de R$ {})",
                    item.ticker(), valorPorAtivo, item.percentual(), valorConsolidado);

            int quantidadeBruta = valorPorAtivo.divide(item.cotacaoAtual(), 0, RoundingMode.DOWN).intValue();
            log.info("Quantidade bruta calculada: {} ações (R$ {} / R$ {})",
                    quantidadeBruta, valorPorAtivo, item.cotacaoAtual());

            int saldoMaster = 0;
            if (!custodiasMaster.isEmpty()) {
                for (Custodia custodia : custodiasMaster) {
                    if (custodia.getTicker().equals(item.ticker())) {
                        saldoMaster = custodia.getQuantidade();
                        log.info("Saldo master encontrado para {}: {} ações", item.ticker(), saldoMaster);
                        break;
                    }
                }
            }

            int quantidadeFinalComprar = quantidadeBruta - saldoMaster;
            log.info("Quantidade final a comprar: {} ações (bruta: {} - saldo master: {})",
                    quantidadeFinalComprar, quantidadeBruta, saldoMaster);

            if (quantidadeFinalComprar <= 0) {
                log.warn("Quantidade final <= 0 para {}. Saldo master suficiente. Pulando compra.", item.ticker());
                continue;
            }

            List<DetalhesOrdemCompra> detalhesOrdens = separarLotePadraoFracionario(item.ticker(), quantidadeFinalComprar);

            BigDecimal valorTotalOrdem = item.cotacaoAtual().multiply(BigDecimal.valueOf(quantidadeFinalComprar));

            OrdemCompraResponseDTO ordemCompra = new OrdemCompraResponseDTO(
                    item.ticker(),
                    quantidadeFinalComprar,
                    detalhesOrdens,
                    item.cotacaoAtual(),
                    valorTotalOrdem
            );

            dtos.add(ordemCompra);
            log.info("Ordem de compra criada para {}: {} ações por R$ {}",
                    item.ticker(), quantidadeFinalComprar, valorTotalOrdem);
        }

        for (OrdemCompraResponseDTO ordem : dtos) {
            OrdemCompra ordemCompra = ordemCompraRepository.save(ordemCompraMapper.mapearParaOrdemCompra(ordem, contaMasterId, dataReferencia));
            entities.add(ordemCompra);
        }

        log.info("=== Total de ordens de compra geradas: {} ===", dtos.size());
        return new CalcularQuantidadeAtivoResponse(
                entities,
                dtos
        );
    }

    private List<DetalhesOrdemCompra> separarLotePadraoFracionario(String ticker, int quantidadeTotal) {
        List<DetalhesOrdemCompra> detalhes = new ArrayList<>();

        if (quantidadeTotal >= 100) {
            int lotePadrao = (quantidadeTotal / 100) * 100;
            int fracionario = quantidadeTotal % 100;

            DetalhesOrdemCompra ordemLotePadrao = new DetalhesOrdemCompra(
                    "LOTE",
                    ticker,
                    lotePadrao
            );
            detalhes.add(ordemLotePadrao);
            log.info("Lote Padrão: {} ações via {} ({} lotes de 100)",
                    lotePadrao, ticker, lotePadrao / 100);

            if (fracionario > 0) {
                DetalhesOrdemCompra ordemFracionaria = new DetalhesOrdemCompra(
                        "FRACIONARIO",
                        ticker + "F",
                        fracionario
                );
                detalhes.add(ordemFracionaria);
                log.info("Fracionário: {} ações via {}", fracionario, ticker + "F");
            }
        } else {
            DetalhesOrdemCompra ordemFracionaria = new DetalhesOrdemCompra(
                    "FRACIONARIO",
                    ticker + "F",
                    quantidadeTotal
            );
            detalhes.add(ordemFracionaria);
            log.info("Fracionário: {} ações via {}", quantidadeTotal, ticker + "F");
        }

        log.info("=== Separação concluída: {} ordem(ns) criada(s) ===", detalhes.size());
        return detalhes;
    }
}
