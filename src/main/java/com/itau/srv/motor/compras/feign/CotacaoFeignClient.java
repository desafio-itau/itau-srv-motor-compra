package com.itau.srv.motor.compras.feign;

import com.itau.srv.motor.compras.dto.cotacao.CotacaoResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${external-endpoints.itau-srv-cotacoes.name}", url = "${external-endpoints.itau-srv-cotacoes.url}")
public interface CotacaoFeignClient {

    @GetMapping("/{ticker}")
    CotacaoResponseDTO obterCotacaoPorTicker(@PathVariable String ticker);
}
