package com.itau.srv.motor.compras.feign;

import com.itau.srv.motor.compras.dto.cliente.ClienteResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "${external-endpoints.itau-srv-clientes.name}", url = "${external-endpoints.itau-srv-clientes.url}")
public interface ClientesFeignClient {

    @GetMapping
    List<ClienteResponseDTO> listarClientesAtivos();
}
