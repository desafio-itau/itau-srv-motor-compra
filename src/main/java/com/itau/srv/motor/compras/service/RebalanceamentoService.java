package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.cesta.CestaResponseDTO;
import com.itau.srv.motor.compras.dto.cliente.ClienteResponseDTO;
import com.itau.srv.motor.compras.dto.cotacao.CotacaoResponseDTO;
import com.itau.srv.motor.compras.dto.ir.IRDedoDuroEventDTO;
import com.itau.srv.motor.compras.dto.itemcesta.ItemCotacaoAtualResponseDTO;
import com.itau.srv.motor.compras.dto.rebalanceamento.*;
import com.itau.srv.motor.compras.feign.CestaFeignClient;
import com.itau.srv.motor.compras.feign.ClientesFeignClient;
import com.itau.srv.motor.compras.feign.CotacaoFeignClient;
import com.itau.srv.motor.compras.kafka.IREventProducer;
import com.itau.srv.motor.compras.mapper.EventoIRMapper;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.model.Rebalanceamento;
import com.itau.srv.motor.compras.model.enums.TipoIR;
import com.itau.srv.motor.compras.model.enums.TipoRebalanceamento;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import com.itau.srv.motor.compras.repository.EventoIRRepository;
import com.itau.srv.motor.compras.repository.RebalanceamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RebalanceamentoService {

    private final CestaFeignClient cestaFeignClient;
    private final ClientesFeignClient clientesFeignClient;
    private final CotacaoFeignClient cotacaoFeignClient;
    private final CustodiaRepository custodiaRepository;
    private final IREventProducer irEventProducer;
    private final EventoIRRepository eventoIRRepository;
    private final EventoIRMapper eventoIRMapper;
    private final RebalanceamentoRepository rebalanceamentoRepository;

    @Transactional
    public void executarRebalanceamento(Long cestaAnteriorId, Long cestaAtualId, LocalDate dataExecucao) {
        log.info("=== INICIANDO REBALANCEAMENTO ===");
        log.info("Cesta Anterior ID: {}", cestaAnteriorId);
        log.info("Cesta Atual ID: {}", cestaAtualId);
        log.info("Data Execução: {}", dataExecucao);

        CestaResponseDTO cestaAnterior = cestaFeignClient.obterCestaPorId(cestaAnteriorId);
        CestaResponseDTO cestaAtual = cestaFeignClient.obterCestaPorId(cestaAtualId);

        log.info("Cesta Anterior: {} (Ativos: {})", cestaAnterior.nome(), cestaAnterior.itens().size());
        log.info("Cesta Atual: {} (Ativos: {})", cestaAtual.nome(), cestaAtual.itens().size());

        Map<String, BigDecimal> ativosAntigos = mapearAtivos(cestaAnterior.itens());
        Map<String, BigDecimal> ativosNovos = mapearAtivos(cestaAtual.itens());

        Set<String> ativosSairam = new HashSet<>(ativosAntigos.keySet());
        ativosSairam.removeAll(ativosNovos.keySet());

        Set<String> ativosEntraram = new HashSet<>(ativosNovos.keySet());
        ativosEntraram.removeAll(ativosAntigos.keySet());

        Set<String> ativosPermaneceram = new HashSet<>(ativosAntigos.keySet());
        ativosPermaneceram.retainAll(ativosNovos.keySet());

        log.info("Ativos que SAÍRAM: {}", ativosSairam);
        log.info("Ativos que ENTRARAM: {}", ativosEntraram);
        log.info("Ativos que PERMANECERAM: {}", ativosPermaneceram);

        List<ClienteResponseDTO> clientesAtivos = clientesFeignClient.listarClientesAtivos();
        log.info("Total de clientes ativos: {}", clientesAtivos.size());

        for (ClienteResponseDTO cliente : clientesAtivos) {
            log.info("\n>>> PROCESSANDO CLIENTE: {} (ID: {})", cliente.nome(), cliente.clienteId());

            BigDecimal totalArrecadado = processarVendasAtivosSairam(
                    cliente, ativosSairam, dataExecucao);

            processarComprasAtivosEntraram(
                    cliente, ativosEntraram, ativosNovos, totalArrecadado, dataExecucao);

            processarRebalanceamentoAtivosPermaneceram(
                    cliente, ativosPermaneceram, ativosAntigos, ativosNovos, dataExecucao);
        }

        log.info("=== REBALANCEAMENTO CONCLUÍDO ===");
    }

    private BigDecimal processarVendasAtivosSairam(
            ClienteResponseDTO cliente, Set<String> ativosSairam, LocalDate dataExecucao) {

        log.info("  [VENDA] Processando vendas de ativos que saíram");
        BigDecimal totalArrecadado = BigDecimal.ZERO;
        List<OperacaoVendaDTO> vendasDoCliente = new ArrayList<>();

        List<Custodia> custodiasCliente = custodiaRepository.findCustodiaCliente(cliente.clienteId());

        for (Custodia custodia : custodiasCliente) {
            String ticker = custodia.getTicker();

            String tickerBase = ticker.replace("F", "");

            if (ativosSairam.contains(tickerBase)) {
                log.info("  [VENDA] Vendendo 100% de {} (Quantidade: {})", ticker, custodia.getQuantidade());

                CotacaoResponseDTO cotacao = cotacaoFeignClient.obterCotacaoPorTicker(tickerBase);
                BigDecimal precoVenda = cotacao.precoFechamento();

                int quantidadeVender = custodia.getQuantidade();
                BigDecimal valorVenda = precoVenda.multiply(BigDecimal.valueOf(quantidadeVender));

                BigDecimal precoMedio = custodia.getPrecoMedio();
                BigDecimal lucro = precoVenda.subtract(precoMedio)
                        .multiply(BigDecimal.valueOf(quantidadeVender));

                log.info("  [VENDA] Preço Venda: R$ {} | Preço Médio: R$ {} | Lucro: R$ {}",
                        precoVenda, precoMedio, lucro);

                OperacaoVendaDTO venda = new OperacaoVendaDTO(
                        cliente.clienteId(),
                        cliente.cpf(),
                        ticker,
                        quantidadeVender,
                        precoVenda,
                        valorVenda,
                        precoMedio,
                        lucro
                );
                vendasDoCliente.add(venda);

                custodia.setQuantidade(0);
                custodiaRepository.save(custodia);
                log.info("  [VENDA] Custodia zerada (ativo saiu da cesta)");

                publicarIRDedoDuroVenda(cliente.clienteId(), cliente.cpf(), ticker,
                        quantidadeVender, precoVenda, dataExecucao);

                salvarRebalanceamento(
                        cliente.clienteId(),
                        TipoRebalanceamento.MUDANCA_CESTA,
                        ticker,
                        null,
                        valorVenda,
                        dataExecucao
                );

                totalArrecadado = totalArrecadado.add(valorVenda);
            }
        }

        log.info("  [VENDA] Total arrecadado com vendas: R$ {}", totalArrecadado);

        if (!vendasDoCliente.isEmpty()) {
            calcularEPublicarIRVendas(cliente, vendasDoCliente, dataExecucao);
        }

        return totalArrecadado;
    }

    private void calcularEPublicarIRVendas(
            ClienteResponseDTO cliente, List<OperacaoVendaDTO> vendas, LocalDate dataExecucao) {

        YearMonth mesReferencia = YearMonth.from(dataExecucao);

        BigDecimal totalVendas = vendas.stream()
                .map(OperacaoVendaDTO::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("  [IR VENDAS] Total vendas do mês: R$ {}", totalVendas);

        BigDecimal limiteIsencao = new BigDecimal("20000.00");
        if (totalVendas.compareTo(limiteIsencao) > 0) {
            BigDecimal lucroTotal = vendas.stream()
                    .map(OperacaoVendaDTO::lucro)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("  [IR VENDAS] Lucro líquido total: R$ {}", lucroTotal);

            if (lucroTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal aliquota = new BigDecimal("0.20"); // 20%
                BigDecimal valorIR = lucroTotal.multiply(aliquota)
                        .setScale(2, RoundingMode.HALF_UP);

                log.info("  [IR VENDAS] IR devido (20%): R$ {}", valorIR);

                List<DetalheVendaDTO> detalhes = vendas.stream()
                        .map(v -> new DetalheVendaDTO(
                                v.ticker(),
                                v.quantidade(),
                                v.precoUnitario(),
                                v.precoMedio(),
                                v.lucro()
                        ))
                        .toList();

                IRVendaMensalEventDTO eventoIRVendas = new IRVendaMensalEventDTO(
                        "IR_VENDA",
                        cliente.clienteId(),
                        cliente.cpf(),
                        mesReferencia.toString(),
                        totalVendas,
                        lucroTotal,
                        aliquota,
                        valorIR,
                        detalhes,
                        LocalDateTime.now()
                );

                irEventProducer.publicarEventoIRVendas(eventoIRVendas);
                log.info("  [IR VENDAS] Evento publicado no Kafka");
            } else {
                log.info("  [IR VENDAS] Lucro <= 0. Sem IR a pagar.");
            }
        } else {
            log.info("  [IR VENDAS] Total vendas <= R$ 20.000,00. Isento de IR.");
        }
    }

    private void processarComprasAtivosEntraram(
            ClienteResponseDTO cliente,
            Set<String> ativosEntraram,
            Map<String, BigDecimal> ativosNovos,
            BigDecimal totalArrecadado,
            LocalDate dataExecucao) {

        log.info("  [COMPRA] Processando compras de ativos que entraram");
        log.info("  [COMPRA] Total disponível: R$ {}", totalArrecadado);

        if (totalArrecadado.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("  [COMPRA] Sem saldo disponível para compras");
            return;
        }

        BigDecimal somaPercentuaisNovos = ativosEntraram.stream()
                .map(ativosNovos::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("  [COMPRA] Soma percentuais novos ativos: {}%", somaPercentuaisNovos);

        for (String ticker : ativosEntraram) {
            BigDecimal percentual = ativosNovos.get(ticker);
            BigDecimal valorInvestir = percentual
                    .divide(somaPercentuaisNovos, 10, RoundingMode.DOWN)
                    .multiply(totalArrecadado);

            log.info("  [COMPRA] {} - Percentual: {}% - Valor a investir: R$ {}",
                    ticker, percentual, valorInvestir);

            CotacaoResponseDTO cotacao = cotacaoFeignClient.obterCotacaoPorTicker(ticker);
            BigDecimal precoCotacao = cotacao.precoFechamento();

            int quantidade = valorInvestir
                    .divide(precoCotacao, 0, RoundingMode.DOWN)
                    .intValue();

            if (quantidade == 0) {
                log.warn("  [COMPRA] Quantidade zero para {}. Pulando.", ticker);
                continue;
            }

            log.info("  [COMPRA] Quantidade a comprar: {} ações de {} a R$ {}",
                    quantidade, ticker, precoCotacao);

            processarCompraLotePadraoEFracionario(
                    cliente, ticker, quantidade, precoCotacao, dataExecucao);
        }
    }

    private void processarCompraLotePadraoEFracionario(
            ClienteResponseDTO cliente,
            String ticker,
            int quantidadeTotal,
            BigDecimal precoCotacao,
            LocalDate dataExecucao) {

        if (quantidadeTotal >= 100) {
            int lotePadrao = (quantidadeTotal / 100) * 100;
            int fracionario = quantidadeTotal % 100;

            log.info("  [COMPRA] Divisão - Lote Padrão: {} | Fracionário: {}", lotePadrao, fracionario);

            if (lotePadrao > 0) {
                atualizarCustodiaCompra(cliente, ticker, lotePadrao, precoCotacao, dataExecucao);
            }

            if (fracionario > 0) {
                atualizarCustodiaCompra(cliente, ticker + "F", fracionario, precoCotacao, dataExecucao);
            }
        } else {
            log.info("  [COMPRA] Quantidade < 100. Tudo fracionário: {}", quantidadeTotal);
            atualizarCustodiaCompra(cliente, ticker + "F", quantidadeTotal, precoCotacao, dataExecucao);
        }
    }


    private void atualizarCustodiaCompra(
            ClienteResponseDTO cliente,
            String ticker,
            int quantidadeCompra,
            BigDecimal precoCompra,
            LocalDate dataExecucao) {

        log.info("  [COMPRA] Atualizando custodia - {} ações de {}", quantidadeCompra, ticker);

        List<Custodia> custodias = custodiaRepository.findCustodiaCliente(cliente.clienteId());
        Custodia custodiaExistente = custodias.stream()
                .filter(c -> c.getTicker().equals(ticker))
                .findFirst()
                .orElse(null);

        if (custodiaExistente == null) {
            Custodia novaCustodia = new Custodia();
            novaCustodia.setTicker(ticker);
            novaCustodia.setQuantidade(quantidadeCompra);
            novaCustodia.setContaGraficaId(cliente.contaGrafica().id());
            novaCustodia.setPrecoMedio(precoCompra);
            custodiaRepository.save(novaCustodia);

            log.info("  [COMPRA] Nova custodia criada - PM: R$ {}", precoCompra);
        } else {
            int qtdAnterior = custodiaExistente.getQuantidade();
            BigDecimal pmAnterior = custodiaExistente.getPrecoMedio();

            BigDecimal numerador = pmAnterior.multiply(BigDecimal.valueOf(qtdAnterior))
                    .add(precoCompra.multiply(BigDecimal.valueOf(quantidadeCompra)));
            BigDecimal denominador = BigDecimal.valueOf((long) qtdAnterior + quantidadeCompra);
            BigDecimal novoPrecoMedio = numerador.divide(denominador, 2, RoundingMode.HALF_UP);

            custodiaExistente.setQuantidade(qtdAnterior + quantidadeCompra);
            custodiaExistente.setPrecoMedio(novoPrecoMedio);
            custodiaRepository.save(custodiaExistente);

            log.info("  [COMPRA] Custodia atualizada - Novo PM: R$ {}", novoPrecoMedio);
        }

        publicarIRDedoDuroCompra(cliente.clienteId(), cliente.cpf(), ticker,
                quantidadeCompra, precoCompra, dataExecucao);

        BigDecimal valorCompra = precoCompra.multiply(BigDecimal.valueOf(quantidadeCompra));
        salvarRebalanceamento(
                cliente.clienteId(),
                com.itau.srv.motor.compras.model.enums.TipoRebalanceamento.MUDANCA_CESTA,
                null,
                ticker,
                valorCompra,
                dataExecucao
        );
    }

    private void processarRebalanceamentoAtivosPermaneceram(
            ClienteResponseDTO cliente,
            Set<String> ativosPermaneceram,
            Map<String, BigDecimal> ativosAntigos,
            Map<String, BigDecimal> ativosNovos,
            LocalDate dataExecucao) {

        log.info("  [REBALANCEAMENTO] Processando ativos que permaneceram");

        List<Custodia> custodiasCliente = custodiaRepository.findCustodiaCliente(cliente.clienteId());
        BigDecimal valorTotalCarteira = BigDecimal.ZERO;

        Map<String, Custodia> custodiasPorTicker = new HashMap<>();

        for (Custodia custodia : custodiasCliente) {
            String tickerBase = custodia.getTicker().replace("F", "");

            CotacaoResponseDTO cotacao = cotacaoFeignClient.obterCotacaoPorTicker(tickerBase);
            BigDecimal valorAtivo = cotacao.precoFechamento()
                    .multiply(BigDecimal.valueOf(custodia.getQuantidade()));

            valorTotalCarteira = valorTotalCarteira.add(valorAtivo);

            custodiasPorTicker.merge(tickerBase, custodia, (c1, c2) -> {
                c1.setQuantidade(c1.getQuantidade() + c2.getQuantidade());
                return c1;
            });
        }

        log.info("  [REBALANCEAMENTO] Valor total da carteira: R$ {}", valorTotalCarteira);

        for (String ticker : ativosPermaneceram) {
            BigDecimal percentualAntigo = ativosAntigos.get(ticker);
            BigDecimal percentualNovo = ativosNovos.get(ticker);

            log.info("  [REBALANCEAMENTO] {} - Percentual antigo: {}% | Percentual novo: {}%",
                    ticker, percentualAntigo, percentualNovo);

            if (percentualAntigo.compareTo(percentualNovo) == 0) {
                log.info("  [REBALANCEAMENTO] Percentual inalterado. Pulando.");
                continue;
            }

            BigDecimal valorAlvo = valorTotalCarteira
                    .multiply(percentualNovo.divide(BigDecimal.valueOf(100), 10, RoundingMode.DOWN));

            CotacaoResponseDTO cotacao = cotacaoFeignClient.obterCotacaoPorTicker(ticker);
            BigDecimal precoCotacao = cotacao.precoFechamento();

            int quantidadeAlvo = valorAlvo
                    .divide(precoCotacao, 0, RoundingMode.DOWN)
                    .intValue();

            Custodia custodiaAtual = custodiasPorTicker.get(ticker);
            int quantidadeAtual = custodiaAtual != null ? custodiaAtual.getQuantidade() : 0;

            log.info("  [REBALANCEAMENTO] Quantidade atual: {} | Quantidade alvo: {}",
                    quantidadeAtual, quantidadeAlvo);

            if (quantidadeAtual > quantidadeAlvo) {
                int quantidadeVender = quantidadeAtual - quantidadeAlvo;
                log.info("  [REBALANCEAMENTO] VENDER {} ações de {}", quantidadeVender, ticker);

                processarVendaRebalanceamento(cliente, ticker, quantidadeVender,
                        custodiaAtual, precoCotacao, dataExecucao);

            } else if (quantidadeAtual < quantidadeAlvo) {
                int quantidadeComprar = quantidadeAlvo - quantidadeAtual;
                log.info("  [REBALANCEAMENTO] COMPRAR {} ações de {}", quantidadeComprar, ticker);

                processarCompraLotePadraoEFracionario(cliente, ticker, quantidadeComprar,
                        precoCotacao, dataExecucao);
            }
        }
    }

    private void processarVendaRebalanceamento(
            ClienteResponseDTO cliente,
            String ticker,
            int quantidadeVender,
            Custodia custodia,
            BigDecimal precoVenda,
            LocalDate dataExecucao) {

        BigDecimal valorVenda = precoVenda.multiply(BigDecimal.valueOf(quantidadeVender));
        BigDecimal precoMedio = custodia.getPrecoMedio();
        BigDecimal lucro = precoVenda.subtract(precoMedio)
                .multiply(BigDecimal.valueOf(quantidadeVender));

        log.info("  [VENDA REBAL] Valor venda: R$ {} | Lucro: R$ {}", valorVenda, lucro);

        int novaQuantidade = custodia.getQuantidade() - quantidadeVender;
        custodia.setQuantidade(novaQuantidade);
        custodiaRepository.save(custodia);

        if (novaQuantidade == 0) {
            log.info("  [VENDA REBAL] Custodia zerada (quantidade chegou a 0)");
        } else {
            log.info("  [VENDA REBAL] Custodia atualizada. Nova quantidade: {}", novaQuantidade);
        }

        publicarIRDedoDuroVenda(cliente.clienteId(), cliente.cpf(), ticker,
                quantidadeVender, precoVenda, dataExecucao);

        salvarRebalanceamento(
                cliente.clienteId(),
                TipoRebalanceamento.DESVIO,
                ticker,
                null,
                valorVenda,
                dataExecucao
        );
    }

    private void publicarIRDedoDuroCompra(
            Long clienteId, String cpf, String ticker,
            int quantidade, BigDecimal precoUnitario, LocalDate dataReferencia) {

        BigDecimal valorOperacao = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        BigDecimal aliquota = new BigDecimal("0.00005");
        BigDecimal valorIR = valorOperacao.multiply(aliquota)
                .setScale(2, RoundingMode.HALF_UP);

        IRDedoDuroEventDTO evento = new IRDedoDuroEventDTO(
                TipoIR.IR_DEDO_DURO.toString(),
                clienteId,
                cpf,
                ticker,
                "COMPRA",
                quantidade,
                precoUnitario,
                valorOperacao,
                aliquota,
                valorIR,
                dataReferencia.atTime(LocalTime.now())
        );

        irEventProducer.publicarEventoIRDedoDuro(evento);
        eventoIRRepository.save(eventoIRMapper.mapearParaEventoIr(evento));

        log.info("  [IR DEDO-DURO] COMPRA - IR: R$ {}", valorIR);
    }

    private void publicarIRDedoDuroVenda(
            Long clienteId, String cpf, String ticker,
            int quantidade, BigDecimal precoUnitario, LocalDate dataReferencia) {

        BigDecimal valorOperacao = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        BigDecimal aliquota = new BigDecimal("0.00005");
        BigDecimal valorIR = valorOperacao.multiply(aliquota)
                .setScale(2, RoundingMode.HALF_UP);

        IRDedoDuroEventDTO evento = new IRDedoDuroEventDTO(
                TipoIR.IR_VENDA.toString(),
                clienteId,
                cpf,
                ticker,
                "VENDA",
                quantidade,
                precoUnitario,
                valorOperacao,
                aliquota,
                valorIR,
                dataReferencia.atTime(LocalTime.now())
        );

        irEventProducer.publicarEventoIRDedoDuro(evento);
        eventoIRRepository.save(eventoIRMapper.mapearParaEventoIr(evento));

        log.info("  [IR DEDO-DURO] VENDA - IR: R$ {}", valorIR);
    }

    private Map<String, BigDecimal> mapearAtivos(List<ItemCotacaoAtualResponseDTO> itens) {
        return itens.stream()
                .collect(Collectors.toMap(
                        ItemCotacaoAtualResponseDTO::ticker,
                        ItemCotacaoAtualResponseDTO::percentual
                ));
    }

    private void salvarRebalanceamento(
            Long clienteId,
            com.itau.srv.motor.compras.model.enums.TipoRebalanceamento tipo,
            String tickerVendido,
            String tickerComprado,
            BigDecimal valorVenda,
            LocalDate dataExecucao) {

        Rebalanceamento rebalanceamento = new Rebalanceamento();
        rebalanceamento.setClienteId(clienteId);
        rebalanceamento.setTipo(tipo);
        rebalanceamento.setTickerVendido(tickerVendido != null ? tickerVendido : "-");
        rebalanceamento.setTickerComprado(tickerComprado != null ? tickerComprado : "-");
        rebalanceamento.setValorVenda(valorVenda);
        rebalanceamento.setDataRebalanceamento(dataExecucao.atTime(LocalTime.now()));

        rebalanceamentoRepository.save(rebalanceamento);

        log.debug("  [REBALANCEAMENTO] Salvo - Cliente: {} | Tipo: {} | Vendido: {} | Comprado: {} | Valor: R$ {}",
                clienteId, tipo,
                tickerVendido != null ? tickerVendido : "N/A",
                tickerComprado != null ? tickerComprado : "N/A",
                valorVenda);
    }

    @Transactional
    public RebalanceamentoDesvioResponseDTO executarRebalanceamentoPorDesvio(
            BigDecimal limiarDesvio, LocalDate dataExecucao) {

        log.info("=== INICIANDO REBALANCEAMENTO POR DESVIO ===");
        log.info("Limiar de desvio: {} pontos percentuais", limiarDesvio);
        log.info("Data execução: {}", dataExecucao);

        CestaResponseDTO cestaAtiva = cestaFeignClient.obterCestaAtiva();
        log.info("Cesta ativa: {} (Ativos: {})", cestaAtiva.nome(), cestaAtiva.itens().size());

        List<ClienteResponseDTO> clientesAtivos = clientesFeignClient.listarClientesAtivos();
        log.info("Total de clientes ativos: {}", clientesAtivos.size());

        int totalClientesRebalanceados = 0;
        int totalOperacoes = 0;
        List<ClienteRebalanceadoDTO> clientesRebalanceados = new ArrayList<>();

        for (ClienteResponseDTO cliente : clientesAtivos) {
            log.info("\n>>> ANALISANDO CLIENTE: {} (ID: {})", cliente.nome(), cliente.clienteId());

            List<OperacaoRebalanceamentoDTO> operacoes =
                    processarRebalanceamentoDesvioCliente(cliente, cestaAtiva, limiarDesvio, dataExecucao);

            if (!operacoes.isEmpty()) {
                totalClientesRebalanceados++;
                totalOperacoes += operacoes.size();

                clientesRebalanceados.add(new ClienteRebalanceadoDTO(
                        cliente.clienteId(),
                        cliente.nome(),
                        operacoes
                ));

                log.info("  [CLIENTE REBALANCEADO] {} operações realizadas", operacoes.size());
            } else {
                log.info("  [SEM DESVIO] Carteira dentro do limiar aceitável");
            }
        }

        log.info("=== REBALANCEAMENTO POR DESVIO CONCLUÍDO ===");
        log.info("Clientes processados: {}", clientesAtivos.size());
        log.info("Clientes rebalanceados: {}", totalClientesRebalanceados);
        log.info("Total de operações: {}", totalOperacoes);

        return new com.itau.srv.motor.compras.dto.rebalanceamento.RebalanceamentoDesvioResponseDTO(
                clientesAtivos.size(),
                totalClientesRebalanceados,
                totalOperacoes,
                clientesRebalanceados,
                String.format("Rebalanceamento por desvio executado com sucesso. %d/%d clientes rebalanceados.",
                        totalClientesRebalanceados, clientesAtivos.size())
        );
    }

    private List<OperacaoRebalanceamentoDTO> processarRebalanceamentoDesvioCliente(
            ClienteResponseDTO cliente,
            CestaResponseDTO cestaAtiva,
            BigDecimal limiarDesvio,
            LocalDate dataExecucao) {

        List<OperacaoRebalanceamentoDTO> operacoes = new ArrayList<>();

        List<Custodia> custodiasCliente = custodiaRepository.findCustodiaCliente(cliente.clienteId());

        custodiasCliente = custodiasCliente.stream()
                .filter(c -> c.getQuantidade() > 0)
                .collect(java.util.stream.Collectors.toList());

        if (custodiasCliente.isEmpty()) {
            log.info("  [SEM CUSTÓDIA] Cliente não possui ações (ou todas zeradas)");
            return operacoes;
        }

        BigDecimal valorTotalCarteira = BigDecimal.ZERO;
        Map<String, Custodia> custodiasPorTicker = new HashMap<>();

        for (Custodia custodia : custodiasCliente) {
            CotacaoResponseDTO cotacao = cotacaoFeignClient.obterCotacaoPorTicker(custodia.getTicker());
            BigDecimal valorAtivo = cotacao.precoFechamento().multiply(BigDecimal.valueOf(custodia.getQuantidade()));
            valorTotalCarteira = valorTotalCarteira.add(valorAtivo);
            custodiasPorTicker.put(custodia.getTicker(), custodia);
        }

        log.info("  [VALOR CARTEIRA] R$ {}", valorTotalCarteira);

        if (valorTotalCarteira.compareTo(BigDecimal.ZERO) == 0) {
            log.info("  [CARTEIRA VAZIA] Valor total zero");
            return operacoes;
        }

        for (ItemCotacaoAtualResponseDTO item : cestaAtiva.itens()) {
            String ticker = item.ticker();
            BigDecimal percentualAlvo = item.percentual();

            Custodia custodia = custodiasPorTicker.get(ticker);
            BigDecimal proporcaoAtual;

            if (custodia == null || custodia.getQuantidade() == 0) {
                proporcaoAtual = BigDecimal.ZERO;
            } else {
                CotacaoResponseDTO cotacao = cotacaoFeignClient.obterCotacaoPorTicker(ticker);
                BigDecimal valorAtivo = cotacao.precoFechamento().multiply(BigDecimal.valueOf(custodia.getQuantidade()));
                proporcaoAtual = valorAtivo.divide(valorTotalCarteira, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            BigDecimal desvio = proporcaoAtual.subtract(percentualAlvo).abs();

            log.info("  [{}] Proporção Atual: {}% | Alvo: {}% | Desvio: {}%",
                    ticker, proporcaoAtual.setScale(2, RoundingMode.HALF_UP),
                    percentualAlvo.setScale(2, RoundingMode.HALF_UP),
                    desvio.setScale(2, RoundingMode.HALF_UP));

            if (desvio.compareTo(limiarDesvio) > 0) {
                log.info("    [DESVIO DETECTADO] {} está fora do limiar de {}%", ticker, limiarDesvio);

                CotacaoResponseDTO cotacao = cotacaoFeignClient.obterCotacaoPorTicker(ticker);
                BigDecimal valorAlvo = valorTotalCarteira.multiply(percentualAlvo).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                int quantidadeAlvo = valorAlvo.divide(cotacao.precoFechamento(), 0, RoundingMode.DOWN).intValue();
                int quantidadeAtual = custodia != null ? custodia.getQuantidade() : 0;

                if (proporcaoAtual.compareTo(percentualAlvo) > 0) {
                    int quantidadeVender = quantidadeAtual - quantidadeAlvo;

                    if (quantidadeVender > 0 && custodia != null) {
                        log.info("    [VENDA] Vender {} ações de {} a R$ {}",
                                quantidadeVender, ticker, cotacao.precoFechamento());

                        BigDecimal valorOperacao = cotacao.precoFechamento()
                                .multiply(BigDecimal.valueOf(quantidadeVender));

                        custodia.setQuantidade(quantidadeAtual - quantidadeVender);
                        custodia.setDataUltimaAtualizacao(dataExecucao.atStartOfDay());
                        custodiaRepository.save(custodia);

                        publicarIRDedoDuroVenda(cliente.clienteId(), cliente.cpf(), ticker,
                                quantidadeVender, cotacao.precoFechamento(), dataExecucao);

                        salvarRebalanceamento(
                                cliente.clienteId(),
                                TipoRebalanceamento.DESVIO,
                                ticker,
                                null,
                                valorOperacao,
                                dataExecucao
                        );

                        operacoes.add(new OperacaoRebalanceamentoDTO(
                                ticker,
                                "VENDA",
                                quantidadeVender,
                                cotacao.precoFechamento(),
                                valorOperacao,
                                proporcaoAtual,
                                percentualAlvo,
                                desvio
                        ));
                    }
                } else {
                    int quantidadeComprar = quantidadeAlvo - quantidadeAtual;

                    if (quantidadeComprar > 0) {
                        log.info("    [COMPRA] Comprar {} ações de {} a R$ {}",
                                quantidadeComprar, ticker, cotacao.precoFechamento());

                        BigDecimal valorOperacao = cotacao.precoFechamento()
                                .multiply(BigDecimal.valueOf(quantidadeComprar));

                        if (custodia == null) {
                            custodia = new Custodia();
                            custodia.setTicker(ticker);
                            custodia.setContaGraficaId(cliente.contaGrafica().id());
                            custodia.setQuantidade(quantidadeComprar);
                            custodia.setPrecoMedio(cotacao.precoFechamento());
                            custodia.setDataUltimaAtualizacao(dataExecucao.atStartOfDay());
                        } else {
                            BigDecimal numerador = custodia.getPrecoMedio()
                                    .multiply(BigDecimal.valueOf(quantidadeAtual))
                                    .add(cotacao.precoFechamento().multiply(BigDecimal.valueOf(quantidadeComprar)));
                            BigDecimal denominador = BigDecimal.valueOf(quantidadeAtual + quantidadeComprar);
                            BigDecimal novoPrecoMedio = numerador.divide(denominador, 2, RoundingMode.HALF_UP);

                            custodia.setQuantidade(quantidadeAtual + quantidadeComprar);
                            custodia.setPrecoMedio(novoPrecoMedio);
                            custodia.setDataUltimaAtualizacao(dataExecucao.atStartOfDay());
                        }

                        custodiaRepository.save(custodia);

                        publicarIRDedoDuroCompra(cliente.clienteId(), cliente.cpf(), ticker,
                                quantidadeComprar, cotacao.precoFechamento(), dataExecucao);

                        salvarRebalanceamento(
                                cliente.clienteId(),
                                TipoRebalanceamento.DESVIO,
                                null,
                                ticker,
                                valorOperacao,
                                dataExecucao
                        );

                        operacoes.add(new OperacaoRebalanceamentoDTO(
                                ticker,
                                "COMPRA",
                                quantidadeComprar,
                                cotacao.precoFechamento(),
                                valorOperacao,
                                proporcaoAtual,
                                percentualAlvo,
                                desvio
                        ));
                    }
                }
            }
        }

        return operacoes;
    }
}
