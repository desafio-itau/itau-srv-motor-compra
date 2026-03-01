package com.itau.srv.motor.compras.dto.cesta;

import com.itau.srv.motor.compras.dto.itemcesta.ItemCotacaoAtualResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public record CestaResponseDTO(
        Long cestaId,
        String nome,
        Boolean ativa,
        LocalDateTime dataCriacao,
        List<ItemCotacaoAtualResponseDTO> itens
) {
}
