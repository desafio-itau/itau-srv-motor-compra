package com.itau.srv.motor.compras.dto.conta;

import java.time.LocalDateTime;

public record ContaGraficaDTO(
        Long id,
        String numeroConta,
        String tipoConta,
        LocalDateTime dataCriacao
) {
}
