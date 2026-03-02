package com.itau.srv.motor.compras.repository;

import com.itau.srv.motor.compras.model.EventoIR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventoIRRepository extends JpaRepository<EventoIR, Long> {
    @Query("SELECT e FROM EventoIR e WHERE e.clienteId = :clienteId AND e.tipo = 'IR_DEDO_DURO'")
    List<EventoIR> findInvestidoPorCliente(Long clienteId);

    @Query("SELECT e FROM EventoIR e WHERE e.clienteId = :clienteId AND e.tipo = 'IR_VENDA'")
    List<EventoIR> findVendidoPorCliente(Long clienteId);
}
