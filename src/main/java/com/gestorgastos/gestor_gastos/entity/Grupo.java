package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    @ManyToOne
    private Usuario creador;

    // Constructor vacío obligatorio para JPA
    public Grupo() {
    }

    // Constructor con los datos del grupo
    public Grupo(String name, LocalDate fechaCreacion, Usuario creador) {
        this.name = name;
        this.fechaCreacion = fechaCreacion;
        this.creador = creador;
    }

    // Getter y Setter de id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Getter y Setter de name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter y Setter de fechaCreacion
    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Métodos alias para compatibilidad
    public LocalDate getFecha_creacion() {
        return fechaCreacion;
    }

    public void setFecha_creacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Getter y Setter de creador
    public Usuario getCreador() {
        return creador;
    }

    public void setCreador(Usuario creador) {
        this.creador = creador;
    }
}
