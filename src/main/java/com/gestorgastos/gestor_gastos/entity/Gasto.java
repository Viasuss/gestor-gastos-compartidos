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

    public long Getid(){
        return this.id;
    }
    public void  Setid(long id){
        this.id=id;
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
    public void setFecha(String fecha) {
        this.fecha = LocalDate.parse(fecha);
    }

    public Usuario getusuario() {
        return usuario;
    }
    public void setusuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Grupo getgrupo() {
        return grupo;
    }
    public void setgrupo(Grupo grupo) {
        this.grupo = grupo;
    }
}



