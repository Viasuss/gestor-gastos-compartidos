package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;
    private LocalDate fecha_creacion;

    @ManyToOne
    private Usuario creador;

    // Constructor vacío obligatorio para JPA
    public Grupo() {
    }

    // Constructor con los datos del grupo
    public Grupo(String name, LocalDate fecha_creacion, Usuario UsuarioPago) {
        this.name = name;
        this.fecha_creacion = fecha_creacion;
        this.creador = UsuarioPago;
    }

    // Getter y Setter de id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // Getter y Setter de name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter y Setter de fecha_creacion
    public LocalDate getFecha_creacion() {
        return fecha_creacion;
    }

    public void setFecha_creacion(LocalDate fecha_creacion) {
        this.fecha_creacion = fecha_creacion;
    }

    // Getter y Setter de creador
    public Usuario getCreador() {
        return creador;
    }

    public void setCreador(Usuario creador) {
        this.creador = creador;
    }
}