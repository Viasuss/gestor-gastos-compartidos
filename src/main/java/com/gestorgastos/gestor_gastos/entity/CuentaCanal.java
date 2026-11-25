package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cuenta_canal",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cuenta_canal_externo",
                columnNames = {"canal", "identificador_externo"}
        )
)
public class CuentaCanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 20, nullable = false)
    private String canal = "TELEGRAM";

    @Column(name = "identificador_externo", nullable = false)
    private Long identificadorExterno;

    @Column(name = "grupo_activo_id")
    private Long grupoActivoId;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_vinculacion", nullable = false)
    private LocalDateTime fechaVinculacion;

    public CuentaCanal() {
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public Long getIdentificadorExterno() { return identificadorExterno; }
    public void setIdentificadorExterno(Long identificadorExterno) { this.identificadorExterno = identificadorExterno; }
    public Long getGrupoActivoId() { return grupoActivoId; }
    public void setGrupoActivoId(Long grupoActivoId) { this.grupoActivoId = grupoActivoId; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public LocalDateTime getFechaVinculacion() { return fechaVinculacion; }
    public void setFechaVinculacion(LocalDateTime fechaVinculacion) { this.fechaVinculacion = fechaVinculacion; }
}
