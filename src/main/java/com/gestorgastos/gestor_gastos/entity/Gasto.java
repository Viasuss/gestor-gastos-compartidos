package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity

public class Gasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Usuario usuario;
    @ManyToOne
    private Grupo grupo;

    private String descripcion;
    private BigDecimal monto;
    private LocalDate fecha;

    public Gasto() {

    }

    public Gasto(String descripcion, BigDecimal monto, LocalDate fecha, Usuario UsuarioPago, Grupo GrupoPertenece) {
        this .descripcion = descripcion;
        this.monto = monto;
        this.fecha = fecha;
        this.usuario = UsuarioPago;
        this.grupo =  GrupoPertenece;
    }

    public long GetId(){
        return this.id;
    }
    public void SetId(long id){
        this.id=id;
    }

    public String GetDescripcion() {
        return descripcion;
    }
    public void SetDescripcion(String descripcion) {
    this.descripcion = descripcion;
    }

    public BigDecimal GetMonto() {
        return monto;
    }
    public void SetMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate GetFecha() {
        return fecha;
    }
    public void SetFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Usuario Getusuario() {
        return usuario;
    }
    public void Setusuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Grupo Getgrupo() {
        return grupo;
    }
    public void Setgrupo(Grupo grupo) {
        this.grupo = grupo;
    }
}



