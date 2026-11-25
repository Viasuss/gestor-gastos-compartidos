package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "consumo_recurso",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_consumo_prov_user_fecha",
                columnNames = {"proveedor", "usuario_id", "fecha"}
        )
)
public class ConsumoRecurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40, nullable = false)
    private String proveedor;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private int solicitudes = 0;

    @Column(name = "tokens_estimados", nullable = false)
    private long tokensEstimados = 0L;

    public ConsumoRecurso() {
    }

    public ConsumoRecurso(String proveedor, Long usuarioId, LocalDate fecha) {
        this.proveedor = proveedor;
        this.usuarioId = usuarioId;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public String getProveedor() {
        return proveedor;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public int getSolicitudes() {
        return solicitudes;
    }

    public void setSolicitudes(int solicitudes) {
        this.solicitudes = solicitudes;
    }

    public long getTokensEstimados() {
        return tokensEstimados;
    }

    public void setTokensEstimados(long tokensEstimados) {
        this.tokensEstimados = tokensEstimados;
    }
}
