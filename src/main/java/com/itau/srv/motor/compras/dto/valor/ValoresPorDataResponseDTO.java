package com.itau.srv.motor.compras.dto.valor;

import java.time.LocalDate;

public record ValoresPorDataResponseDTO(
        LocalDate dataEvento,
        ValoresResponseDTO valores
) {
}
