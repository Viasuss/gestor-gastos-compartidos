-- Migracion inicial versionada para Partly (B1.6 / HA-007)
-- Preparada para MySQL 8 Community; validar en instancia desechable antes de produccion.

CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password VARCHAR(255) NOT NULL,
    google_sub VARCHAR(255) NULL,
    auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_google_sub UNIQUE (google_sub)
);

CREATE TABLE IF NOT EXISTS grupo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(60) NOT NULL,
    fecha_creacion DATE NOT NULL,
    creador_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_grupo_creador FOREIGN KEY (creador_id) REFERENCES usuario (id)
);

CREATE TABLE IF NOT EXISTS miembro_grupo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    grupo_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_miembro_usuario_grupo UNIQUE (usuario_id, grupo_id),
    CONSTRAINT fk_miembro_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_miembro_grupo FOREIGN KEY (grupo_id) REFERENCES grupo (id)
);

CREATE TABLE IF NOT EXISTS gasto (
    id BIGINT NOT NULL AUTO_INCREMENT,
    grupo_id BIGINT NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    monto DECIMAL(19, 2) NOT NULL,
    fecha DATE NOT NULL,
    idempotency_key VARCHAR(64) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_gasto_grupo_idempotency UNIQUE (grupo_id, idempotency_key),
    CONSTRAINT fk_gasto_grupo FOREIGN KEY (grupo_id) REFERENCES grupo (id)
);

CREATE TABLE IF NOT EXISTS pago_gasto (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gasto_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    monto DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pago_gasto FOREIGN KEY (gasto_id) REFERENCES gasto (id),
    CONSTRAINT fk_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE IF NOT EXISTS gasto_participante (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    grupo_id BIGINT NOT NULL,
    gasto_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_participante_gasto_usuario UNIQUE (gasto_id, usuario_id),
    CONSTRAINT fk_participante_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_participante_grupo FOREIGN KEY (grupo_id) REFERENCES grupo (id),
    CONSTRAINT fk_participante_gasto FOREIGN KEY (gasto_id) REFERENCES gasto (id)
);
