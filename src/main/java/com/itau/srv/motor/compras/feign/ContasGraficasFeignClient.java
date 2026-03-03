package com.itau.srv.motor.compras.feign;

import com.itau.srv.motor.compras.dto.conta.ContaGraficaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${external-endpoints.itau-srv-conta-grafica.name}", url = "${external-endpoints.itau-srv-conta-grafica.url}")
public interface ContasGraficasFeignClient {

    @GetMapping("/{id}")
    ContaGraficaDTO buscarConta(@PathVariable Long id);
}
