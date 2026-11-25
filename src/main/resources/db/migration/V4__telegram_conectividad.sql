-- Migracion B4: Conectividad Telegram Bot API (NEXUS)
CREATE TABLE IF NOT EXISTS cuenta_canal (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    canal VARCHAR(20) NOT NULL,
    identificador_externo BIGINT NOT NULL,
    grupo_activo_id BIGINT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_vinculacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cuenta_canal_externo UNIQUE (canal, identificador_externo),
    CONSTRAINT fk_cuenta_canal_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    INDEX idx_cuenta_canal_usuario (usuario_id, canal, activa)
);

CREATE TABLE IF NOT EXISTS codigo_vinculacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    codigo_hash VARCHAR(64) NOT NULL,
    canal VARCHAR(20) NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_expiracion DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_codigo_hash UNIQUE (codigo_hash),
    CONSTRAINT fk_codigo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE IF NOT EXISTS vinculacion_chat_grupo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chat_id BIGINT NOT NULL,
    grupo_id BIGINT NOT NULL,
    vinculado_por_id BIGINT NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_vinculacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vinculacion_chat UNIQUE (chat_id),
    CONSTRAINT fk_vinc_grupo FOREIGN KEY (grupo_id) REFERENCES grupo (id),
    CONSTRAINT fk_vinc_usuario FOREIGN KEY (vinculado_por_id) REFERENCES usuario (id)
);

CREATE TABLE IF NOT EXISTS telegram_update_procesado (
    update_id BIGINT NOT NULL,
    fecha_procesamiento DATETIME(6) NOT NULL,
    PRIMARY KEY (update_id)
);
