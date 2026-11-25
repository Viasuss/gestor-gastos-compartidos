package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "borrador_operacion",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_borrador_usuario_idempotency",
                columnNames = {"usuario_id", "idempotency_key"}
        )
)
public class BorradorOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(name = "tipo_operacion", length = 30, nullable = false)
    private String tipoOperacion = "REGISTRO_GASTO";

    @Column(length = 100, nullable = false)
    private String concepto;

    @Column(name = "monto_cop", precision = 19, scale = 2)
    private BigDecimal montoCop;

    @Column(name = "categoria_sugerida", length = 50)
    private String categoriaSugerida;

    @Column(name = "fecha_sugerida")
    private LocalDate fechaSugerida;

    @Column(name = "mensaje_original", length = 500)
    private String mensajeOriginal;

    @Column(name = "datos_estructurados", length = 2000)
    private String datosEstructurados;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private EstadoBorrador estado;

    @Column(length = 30, nullable = false)
    private String origen = "WEB_ASISTENTE";

    @Column(name = "proveedor_utilizado", length = 30, nullable = false)
    private String proveedorUtilizado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "gasto_confirmado_id")
    private Long gastoConfirmadoId;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public BorradorOperacion() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { this.grupo = grupo; }
    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public BigDecimal getMontoCop() { return montoCop; }
    public void setMontoCop(BigDecimal montoCop) { this.montoCop = montoCop; }
    public String getCategoriaSugerida() { return categoriaSugerida; }
    public void setCategoriaSugerida(String categoriaSugerida) { this.categoriaSugerida = categoriaSugerida; }
    public LocalDate getFechaSugerida() { return fechaSugerida; }
    public void setFechaSugerida(LocalDate fechaSugerida) { this.fechaSugerida = fechaSugerida; }
    public String getMensajeOriginal() { return mensajeOriginal; }
    public void setMensajeOriginal(String mensajeOriginal) { this.mensajeOriginal = mensajeOriginal; }
    public String getDatosEstructurados() { return datosEstructurados; }
    public void setDatosEstructurados(String datosEstructurados) { this.datosEstructurados = datosEstructurados; }
    public EstadoBorrador getEstado() { return estado; }
    public void setEstado(EstadoBorrador estado) { this.estado = estado; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getProveedorUtilizado() { return proveedorUtilizado; }
    public void setProveedorUtilizado(String proveedorUtilizado) { this.proveedorUtilizado = proveedorUtilizado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Long getGastoConfirmadoId() { return gastoConfirmadoId; }
    public void setGastoConfirmadoId(Long gastoConfirmadoId) { this.gastoConfirmadoId = gastoConfirmadoId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
