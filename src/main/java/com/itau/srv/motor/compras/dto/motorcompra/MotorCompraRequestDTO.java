package com.itau.srv.motor.compras.dto.motorcompra;

import java.time.LocalDate;

public record MotorCompraRequestDTO(
        LocalDate dataReferencia
) {
}
