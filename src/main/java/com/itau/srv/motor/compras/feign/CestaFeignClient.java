package com.itau.srv.motor.compras.feign;

import com.itau.srv.motor.compras.dto.cesta.CestaResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "${external-endpoints.itau-srv-cesta.name}", url = "${external-endpoints.itau-srv-cesta.url}")
public interface CestaFeignClient {

    @GetMapping("/cesta/atual")
    CestaResponseDTO obterCestaAtiva();
}
