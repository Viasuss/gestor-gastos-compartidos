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

    @Enumerated(EnumType.STRING)
    private EstadoMiembro estado;

    // Constructor vacío obligatorio para JPA
    public MiembroGrupo() {
    }

   public MiembroGrupo(Usuario UsuarioPago, Grupo GrupoPertenece,  EstadoMiembro estado) {
        this.usuario = UsuarioPago;
        this.grupo = GrupoPertenece;
        this.estado = estado;
   }

    // Getter y Setter de id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Getter y Setter de usuario
    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    // Getter y Setter de grupo
    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }

    public EstadoMiembro getEstado() {
        return estado;
    }
    public void setEstado(EstadoMiembro estado) {
        this.estado = estado;
    }


}
