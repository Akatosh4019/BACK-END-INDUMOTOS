CREATE TABLE token_revocado (
    token_hash CHAR(64) PRIMARY KEY,
    expira_en TIMESTAMP WITH TIME ZONE NOT NULL,
    revocado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_token_revocado_expira_en ON token_revocado(expira_en);
