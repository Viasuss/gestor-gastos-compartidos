-- Migracion B3: Consumo de recursos IA y Bandeja inteligente de borradores (NEXUS)
CREATE TABLE IF NOT EXISTS consumo_recurso (
    id BIGINT NOT NULL AUTO_INCREMENT,
    proveedor VARCHAR(40) NOT NULL,
    usuario_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    solicitudes INT NOT NULL DEFAULT 0,
    tokens_estimados BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_consumo_prov_user_fecha UNIQUE (proveedor, usuario_id, fecha)
);

CREATE TABLE IF NOT EXISTS borrador_operacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    grupo_id BIGINT NOT NULL,
    tipo_operacion VARCHAR(30) NOT NULL,
    concepto VARCHAR(100) NOT NULL,
    monto_cop DECIMAL(19, 2) NULL,
    categoria_sugerida VARCHAR(50) NULL,
    fecha_sugerida DATE NULL,
    mensaje_original VARCHAR(500) NULL,
    datos_estructurados VARCHAR(2000) NULL,
    estado VARCHAR(30) NOT NULL,
    origen VARCHAR(30) NOT NULL,
    proveedor_utilizado VARCHAR(30) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    fecha_resolucion DATETIME(6) NULL,
    fecha_expiracion DATETIME(6) NOT NULL,
    idempotency_key VARCHAR(64) NULL,
    gasto_confirmado_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_borrador_usuario_idempotency UNIQUE (usuario_id, idempotency_key),
    CONSTRAINT fk_borrador_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_borrador_grupo FOREIGN KEY (grupo_id) REFERENCES grupo (id),
    INDEX idx_borrador_usuario_estado (usuario_id, estado)
);
