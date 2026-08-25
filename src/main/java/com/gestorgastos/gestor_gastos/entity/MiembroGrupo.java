package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

@Entity
public class MiembroGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Usuario usuario;

    @ManyToOne
    private Grupo grupo;


    // Constructor vacío obligatorio para JPA
    public MiembroGrupo() {
    }

   public MiembroGrupo(Usuario UsuarioPago, Grupo GrupoPertenece) {
        this.usuario = UsuarioPago;
        this.grupo = GrupoPertenece;
   }

    // Getter y Setter de id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Getter y Setter de usuario
    public Usuario getusuario() {
        return usuario;
    }

    public void setusuario(Usuario usuario) {
        this.usuario = usuario;
    }

    // Getter y Setter de grupo
    public Grupo getgrupo() {
        return grupo;
    }

    public void setgrupo(Grupo grupo) {
        this.grupo = grupo;
    }
}
