package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResiduoService {

    private final CustodiaRepository custodiaRepository;

    @Value("${motor.compras.conta-master-id}")
    private Long contaMasterId;

    @Transactional
    public void atualizarResiduosCustodiaMaster(String ticker, int residuo, BigDecimal precoUnitario, List<Custodia> custodiasMaster) {
        log.info("Resíduo a processar: {} ações", residuo);

        Custodia custodiaMasterExistente = null;
        for (Custodia custodia : custodiasMaster) {
            if (custodia.getTicker().equals(ticker)) {
                custodiaMasterExistente = custodia;
                break;
            }
        }

        if (residuo > 0) {
            if (custodiaMasterExistente != null) {
                log.info("Custodia master existente encontrada. Quantidade anterior: {}", custodiaMasterExistente.getQuantidade());

                custodiaMasterExistente.setQuantidade(residuo);
                custodiaMasterExistente.setPrecoMedio(precoUnitario);

                custodiaRepository.save(custodiaMasterExistente);
                log.info("✓ Custodia master ATUALIZADA: {} ações de {} com PM = R$ {}",
                        residuo, ticker, precoUnitario);

            } else {
                log.info("Custodia master não existe. Criando nova.");

                Custodia novaCustodiaMaster = new Custodia();
                novaCustodiaMaster.setTicker(ticker);
                novaCustodiaMaster.setQuantidade(residuo);
                novaCustodiaMaster.setContaGraficaId(contaMasterId);
                novaCustodiaMaster.setPrecoMedio(precoUnitario);

                custodiaRepository.save(novaCustodiaMaster);
                log.info("✓ Custodia master CRIADA: {} ações de {} com PM = R$ {}",
                        residuo, ticker, precoUnitario);

                custodiasMaster.add(novaCustodiaMaster);
            }

        } else if (residuo == 0 && custodiaMasterExistente != null) {
            log.info("Resíduo = 0 e custodia master existe. Deletando.");

            custodiaRepository.delete(custodiaMasterExistente);
            custodiasMaster.remove(custodiaMasterExistente);

            log.info("✓ Custodia master DELETADA para {} (resíduo zerado)", ticker);

        } else {
            log.info("Resíduo = 0 e custodia master não existe. Nada a fazer.");
        }

        log.info("=== Resíduo de {} processado com sucesso ===", ticker);
    }
}
