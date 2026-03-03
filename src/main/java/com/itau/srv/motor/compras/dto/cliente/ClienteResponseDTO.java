package com.itau.srv.motor.compras.dto.cliente;


import com.itau.srv.motor.compras.dto.conta.ContaGraficaDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteResponseDTO(
        Long clienteId,
        String nome,
        String cpf,
        String email,
        BigDecimal valorMensal,
        Boolean ativo,
        LocalDateTime dataAdesao,
        ContaGraficaDTO contaGrafica
) {
}
