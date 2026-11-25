# Modelo de Datos Final — NEXUS

**Versión:** `v1.0-rc1` (Migraciones Flyway `V1`–`V4`)

---

## 1. Diagrama Entidad-Relación (Basado en `V1`–`V4`)

```mermaid
erDiagram
    USUARIO ||--o{ GRUPO : crea
    USUARIO ||--o{ MIEMBRO_GRUPO : pertenece
    GRUPO ||--o{ MIEMBRO_GRUPO : agrupa
    GRUPO ||--o{ GASTO : registra
    GASTO ||--o{ PAGO_GASTO : financiado_por
    USUARIO ||--o{ PAGO_GASTO : paga
    GASTO ||--o{ GASTO_PARTICIPANTE : involucra
    USUARIO ||--o{ GASTO_PARTICIPANTE : participa
    GRUPO ||--o{ LIQUIDACION : contiene
    USUARIO ||--o{ LIQUIDACION : paga_o_recibe
    USUARIO ||--o{ BORRADOR_OPERACION : solicita
    GRUPO ||--o{ BORRADOR_OPERACION : destino
    USUARIO ||--o{ CUENTA_CANAL : vincula
    USUARIO ||--o{ CODIGO_VINCULACION : genera
    GRUPO ||--o{ VINCULACION_CHAT_GRUPO : enlaza

    USUARIO {
        bigint id PK
        varchar name
        varchar email UK
        varchar password
        varchar google_sub UK
        varchar auth_provider
        boolean email_verified
    }
    GRUPO {
        bigint id PK
        varchar name
        date fecha_creacion
        bigint creador_id FK
    }
    GASTO {
        bigint id PK
        bigint grupo_id FK
        varchar descripcion
        decimal monto
        date fecha
        varchar idempotency_key UK
    }
    LIQUIDACION {
        bigint id PK
        bigint grupo_id FK
        bigint pagador_id FK
        bigint receptor_id FK
        decimal monto
        varchar estado
        varchar metodo_pago
        varchar idempotency_key UK
        bigint version
    }
    BORRADOR_OPERACION {
        bigint id PK
        bigint usuario_id FK
        bigint grupo_id FK
        varchar tipo_operacion
        varchar concepto
        decimal monto_cop
        varchar estado
        varchar origen
        varchar proveedor_utilizado
        varchar idempotency_key UK
        bigint version
    }
    CONSUMO_RECURSO {
        bigint id PK
        varchar proveedor
        bigint usuario_id
        date fecha
        int solicitudes
        bigint tokens_estimados
    }
    CUENTA_CANAL {
        bigint id PK
        bigint usuario_id FK
        varchar canal
        bigint identificador_externo
        bigint grupo_activo_id
        boolean activa
    }
    CODIGO_VINCULACION {
        bigint id PK
        bigint usuario_id FK
        varchar codigo_hash UK
        varchar canal
        boolean usado
        bigint version
    }
    TELEGRAM_UPDATE_PROCESADO {
        bigint update_id PK
        datetime fecha_procesamiento
    }
```

## 2. Resumen de Migraciones Flyway

| Versión | Archivo | Contenido principal |
|---|---|---|
| `V1` | `V1__baseline_schema.sql` | `usuario`, `grupo`, `miembro_grupo`, `gasto`, `pago_gasto`, `gasto_participante` |
| `V2` | `V2__liquidaciones.sql` | `liquidacion` con `DECIMAL(19,2)`, `idempotency_key`, `version` e índices |
| `V3` | `V3__borradores_y_recursos.sql` | `consumo_recurso` y `borrador_operacion` con `version` e idempotencia |
| `V4` | `V4__telegram_conectividad.sql` | `cuenta_canal`, `codigo_vinculacion`, `vinculacion_chat_grupo`, `telegram_update_procesado` |
