package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.agrupamento.AgrupamentoResponseDTO;
import com.itau.srv.motor.compras.dto.agrupamento.ClienteAgrupamentoDTO;
import com.itau.srv.motor.compras.dto.cliente.ClienteResponseDTO;
import com.itau.srv.motor.compras.feign.ClientesFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteService {

    private final ClientesFeignClient clientesFeignClient;

    @Transactional
    public AgrupamentoResponseDTO agruparClientes() {
        List<ClienteAgrupamentoDTO> clientesAgrupados = new ArrayList<>();
        BigDecimal valorConsolidado = BigDecimal.ZERO;

        List<ClienteResponseDTO> clientesAtivos = clientesFeignClient.listarClientesAtivos();

        for (ClienteResponseDTO cliente : clientesAtivos) {
            BigDecimal valorAportado = cliente.valorMensal().divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

            ClienteAgrupamentoDTO clienteAgrupado = new ClienteAgrupamentoDTO(
                    cliente.clienteId(),
                    cliente.nome(),
                    cliente.cpf(),
                    cliente.contaGrafica().id(),
                    valorAportado
            );

            clientesAgrupados.add(clienteAgrupado);
            valorConsolidado = valorConsolidado.add(valorAportado);
        }

        return new AgrupamentoResponseDTO(valorConsolidado, clientesAgrupados);
    }
}
