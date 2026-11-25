package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "liquidacion",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_liquidacion_grupo_idempotency",
                columnNames = {"grupo_id", "idempotency_key"}
        )
)
public class Liquidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pagador_id", nullable = false)
    private Usuario pagador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EstadoLiquidacion estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 30, nullable = false)
    private MetodoPago metodoPago;

    @Column(length = 250)
    private String nota;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Liquidacion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }

    public Usuario getPagador() {
        return pagador;
    }

    public void setPagador(Usuario pagador) {
        this.pagador = pagador;
    }

    public Usuario getReceptor() {
        return receptor;
    }

    public void setReceptor(Usuario receptor) {
        this.receptor = receptor;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public EstadoLiquidacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoLiquidacion estado) {
        this.estado = estado;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
