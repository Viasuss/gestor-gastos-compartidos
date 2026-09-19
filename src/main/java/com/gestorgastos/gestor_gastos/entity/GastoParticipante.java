package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

@Entity

public class GastoParticipante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;

    @ManyToOne
    private Usuario usuario;
    @ManyToOne
    private Grupo grupo;
    @ManyToOne
    private Gasto gasto;

    public GastoParticipante(){
    }
    public GastoParticipante(Usuario UsuarioPago, Grupo GrupoPertenece, Gasto gasto){
        this.usuario = UsuarioPago;
        this.grupo = GrupoPertenece ;
        this.gasto = gasto;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
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