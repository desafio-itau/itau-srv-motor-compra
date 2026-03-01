package com.itau.srv.motor.compras.repository;

import com.itau.srv.motor.compras.model.OrdemCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface OrdemCompraRepository extends JpaRepository<OrdemCompra, Long> {
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM OrdemCompra o " +
           "WHERE FUNCTION('DATE', o.dataExecucao) = :dataExecucao")
    boolean existsByDataExecucao(@Param("dataExecucao") LocalDate dataExecucao);
}
