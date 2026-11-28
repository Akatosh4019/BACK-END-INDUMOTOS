CREATE TABLE sesion_renovable (
    id UUID PRIMARY KEY,
    familia UUID NOT NULL,
    id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario),
    token_hash CHAR(64) NOT NULL UNIQUE,
    sesion_version BIGINT NOT NULL,
    expira_en TIMESTAMPTZ NOT NULL,
    limite_en TIMESTAMPTZ NOT NULL,
    usado_en TIMESTAMPTZ,
    revocado_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_sesion_renovable_familia ON sesion_renovable(familia);
CREATE INDEX ix_sesion_renovable_usuario ON sesion_renovable(id_usuario);
