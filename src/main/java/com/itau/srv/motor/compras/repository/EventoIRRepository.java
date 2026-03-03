package com.itau.srv.motor.compras.repository;

import com.itau.srv.motor.compras.dto.ir.EventoIRSumarioDTO;
import com.itau.srv.motor.compras.model.EventoIR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface EventoIRRepository extends JpaRepository<EventoIR, Long> {
    @Query("SELECT e FROM EventoIR e WHERE e.clienteId = :clienteId AND e.tipo = 'IR_DEDO_DURO'")
    List<EventoIR> findInvestidoPorCliente(Long clienteId);

    @Query("SELECT e FROM EventoIR e WHERE e.clienteId = :clienteId AND e.tipo = 'IR_VENDA'")
    List<EventoIR> findVendidoPorCliente(Long clienteId);

    @Query("SELECT e FROM EventoIR e WHERE e.clienteId = :clienteId AND CAST(e.dataEvento AS LocalDate) <= :data AND e.tipo = 'IR_DEDO_DURO'")
    List<EventoIR> findInvestidoPorClienteEData(Long clienteId, LocalDate data);

    @Query("SELECT e FROM EventoIR e WHERE e.clienteId = :clienteId AND CAST(e.dataEvento AS LocalDate) <= :data AND e.tipo = 'IR_VENDA'")
    List<EventoIR> findVendidoPorClienteEData(Long clienteId, LocalDate data);

    @Query("""
        SELECT new com.itau.srv.motor.compras.dto.ir.EventoIRSumarioDTO(
            CAST(e.dataEvento AS LocalDate),
            COALESCE(SUM(CASE WHEN e.tipo = 'IR_DEDO_DURO' THEN e.valorBase ELSE 0 END), 0)
        )
        FROM EventoIR e
        WHERE e.clienteId = :clienteId
        GROUP BY CAST(e.dataEvento AS LocalDate)
        ORDER BY CAST(e.dataEvento AS LocalDate) ASC
    """)
    List<EventoIRSumarioDTO> findSumarioEventosPorDataECliente(Long clienteId);

}
