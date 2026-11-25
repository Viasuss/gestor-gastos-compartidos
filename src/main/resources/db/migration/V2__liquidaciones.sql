-- Migracion B2: Modulo de liquidaciones y confirmacion bilateral (NEXUS)
CREATE TABLE IF NOT EXISTS liquidacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    grupo_id BIGINT NOT NULL,
    pagador_id BIGINT NOT NULL,
    receptor_id BIGINT NOT NULL,
    monto DECIMAL(19, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    metodo_pago VARCHAR(30) NOT NULL,
    nota VARCHAR(250) NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_resolucion DATETIME(6) NULL,
    idempotency_key VARCHAR(64) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_liquidacion_grupo_idempotency UNIQUE (grupo_id, idempotency_key),
    CONSTRAINT fk_liquidacion_grupo FOREIGN KEY (grupo_id) REFERENCES grupo (id),
    CONSTRAINT fk_liquidacion_pagador FOREIGN KEY (pagador_id) REFERENCES usuario (id),
    CONSTRAINT fk_liquidacion_receptor FOREIGN KEY (receptor_id) REFERENCES usuario (id),
    INDEX idx_liquidacion_grupo_estado (grupo_id, estado),
    INDEX idx_liquidacion_grupo_fecha (grupo_id, fecha_creacion)
);
