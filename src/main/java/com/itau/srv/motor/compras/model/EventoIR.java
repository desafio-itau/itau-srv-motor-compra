package com.itau.srv.motor.compras.model;

import com.itau.srv.motor.compras.model.enums.TipoIR;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_ir")
@Setter
@Getter
public class EventoIR {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    private TipoIR tipo;

    @Column(nullable = false)
    private BigDecimal valorBase;

    @Column(name = "valor_ir", nullable = false)
    private BigDecimal valorIR;

    @Column(nullable = false)
    private Boolean publicadoKafka;

    @Column(nullable = false)
    private LocalDateTime dataEvento;
}
