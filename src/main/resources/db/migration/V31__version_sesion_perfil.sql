-- Una versión por cuenta invalida sus sesiones al cambiar credenciales o estado.
ALTER TABLE usuario ADD COLUMN sesion_version BIGINT NOT NULL DEFAULT 0
    CHECK (sesion_version >= 0);
