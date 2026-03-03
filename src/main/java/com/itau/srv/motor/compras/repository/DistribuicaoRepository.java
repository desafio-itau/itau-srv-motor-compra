package com.itau.srv.motor.compras.repository;

import com.itau.srv.motor.compras.model.Distribuicao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistribuicaoRepository extends JpaRepository<Distribuicao, Long> {
}
