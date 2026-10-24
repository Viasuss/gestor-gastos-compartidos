package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

@Entity

public class GastoParticipante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Usuario usuario;
    @ManyToOne
    private Grupo grupo;
    @ManyToOne
    private Gasto gasto;

    public GastoParticipante(){
    }
    public GastoParticipante(Usuario usuario, Grupo grupo, Gasto gasto){
        this.usuario = usuario;
        this.grupo = grupo;
        this.gasto = gasto;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Grupo getGrupo() {
        return grupo;
    }
    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }

    public Gasto getGasto() {
        return gasto;
    }
    public void setGasto(Gasto gasto) {
        this.gasto = gasto;
    }
}
