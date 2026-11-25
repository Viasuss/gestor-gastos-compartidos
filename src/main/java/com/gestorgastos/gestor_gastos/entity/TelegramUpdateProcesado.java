package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_update_procesado")
public class TelegramUpdateProcesado {

    @Id
    @Column(name = "update_id")
    private Long updateId;

    @Column(name = "fecha_procesamiento", nullable = false)
    private LocalDateTime fechaProcesamiento;

    public TelegramUpdateProcesado() {
    }

    public TelegramUpdateProcesado(Long updateId, LocalDateTime fechaProcesamiento) {
        this.updateId = updateId;
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public Long getUpdateId() { return updateId; }
    public LocalDateTime getFechaProcesamiento() { return fechaProcesamiento; }
}
