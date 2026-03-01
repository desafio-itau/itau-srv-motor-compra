package com.itau.srv.motor.compras.repository;

import com.itau.srv.motor.compras.model.Custodia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustodiaRepository extends JpaRepository<Custodia, Long> {
    @Query(value = "SELECT c.* FROM custodias c "+
                    "JOIN contas_graficas cg ON c.conta_grafica_id = cg.id " +
                    "WHERE cg.tipo = 'MASTER'", nativeQuery = true)
    List<Custodia> findCustodiaMaster();


    @Query(value = "SELECT c.* FROM custodias c " +
                    "JOIN contas_graficas cg ON c.conta_grafica_id = cg.id " +
                    "WHERE cg.tipo = 'FILHOTE' AND cg.cliente_id = :clienteId", nativeQuery = true)
    List<Custodia> findCustodiaCliente(Long clienteId);
}
