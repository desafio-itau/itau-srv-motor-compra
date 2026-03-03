package com.itau.srv.motor.compras.dto.ir;

import java.time.LocalDateTime;

public record RebalancementoEventDTO(
        Long cestaAnteriorId,
        Long cestaAtualId,
        LocalDateTime dataExecucao
) {
}

