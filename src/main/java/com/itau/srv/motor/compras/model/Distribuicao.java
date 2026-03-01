package com.itau.srv.motor.compras.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "distribuicoes")
@Setter
@Getter
public class Distribuicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ordem_compra_id")
    private OrdemCompra ordemCompra;

    @ManyToOne
    @JoinColumn(name = "custodia_filhote_id")
    private Custodia custodia;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private LocalDateTime dataDistribuicao;
}
