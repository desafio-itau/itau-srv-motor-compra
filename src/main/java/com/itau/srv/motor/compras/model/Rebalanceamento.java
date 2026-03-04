package com.itau.srv.motor.compras.model;

import com.itau.srv.motor.compras.model.enums.TipoRebalanceamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rebalanceamentos")
@Setter
@Getter
public class Rebalanceamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    private TipoRebalanceamento tipo;

    @Column(nullable = false)
    private String tickerVendido;

    @Column(nullable = false)
    private String tickerComprado;

    @Column(nullable = false)
    private BigDecimal valorVenda;

    @Column(nullable = false)
    private LocalDateTime dataRebalanceamento;
}
