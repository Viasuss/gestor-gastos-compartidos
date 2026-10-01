package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity

public class Gasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // ELIMINADO (Sprint 6, ampliación): antes había un único
    // "private Usuario usuario;" que representaba "quién pagó".
    // Ahora un gasto puede tener VARIOS pagadores, así que esa
    // información ya no vive aquí: vive en la nueva tabla PagoGasto,
    // con una fila por cada persona que puso dinero en este gasto.

    @ManyToOne
    private Grupo grupo;

    private String descripcion;
    private BigDecimal monto;
    private LocalDate fecha;

    public Gasto() {

    }

    public Gasto(String descripcion, BigDecimal monto, LocalDate fecha, Grupo GrupoPertenece) {
        this.descripcion = descripcion;
        this.monto = monto;
        this.fecha = fecha;
        this.grupo = GrupoPertenece;
    }

    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }
    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Grupo getGrupo() {
        return grupo;
    }
    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }
}