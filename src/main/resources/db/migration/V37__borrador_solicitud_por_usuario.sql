CREATE TABLE borrador_solicitud (
    usuario VARCHAR(120) PRIMARY KEY,
    contenido JSONB NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
