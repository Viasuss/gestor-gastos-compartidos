package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

// NUEVO (Sprint 6, ampliación): representa UN pago dentro de un gasto.
// Antes, Gasto tenía un solo "usuario" (el pagador). Ahora un gasto
// puede tener VARIOS pagadores, cada uno con su propio monto —
// por ejemplo, un gasto de $50.000 puede tener 2 filas de PagoGasto:
// una de $25.000 (Johan) y otra de $25.000 (Wilmer).
//
// La suma de todos los PagoGasto de un mismo Gasto debe ser igual
// al monto total de ese Gasto (esto se valida en el controlador
// antes de guardar).
@Entity
public class PagoGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Gasto gasto;

    @ManyToOne
    private Usuario usuario;

    private BigDecimal monto;

    public PagoGasto() {
    }

    public PagoGasto(Gasto gasto, Usuario usuario, BigDecimal monto) {
        this.gasto = gasto;
        this.usuario = usuario;
        this.monto = monto;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Gasto getGasto() {
        return gasto;
    }

    public void setGasto(Gasto gasto) {
        this.gasto = gasto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }
}
