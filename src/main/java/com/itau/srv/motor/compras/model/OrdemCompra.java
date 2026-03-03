package com.itau.srv.motor.compras.model;

import com.itau.srv.motor.compras.model.enums.TipoMercado;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ordens_compra")
@Setter
@Getter
public class OrdemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conta_master_id", nullable = false)
    private Long contaMasterId;

    @Column(length = 10, nullable = false)
    private String ticker;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Enumerated(EnumType.STRING)
    private TipoMercado tipoMercado;

    @Column(nullable = false)
    private LocalDateTime dataExecucao;
}
