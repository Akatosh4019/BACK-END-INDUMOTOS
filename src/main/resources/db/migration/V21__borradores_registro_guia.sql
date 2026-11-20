CREATE TABLE registro_guia (
    id_llegada BIGINT PRIMARY KEY REFERENCES llegada(id_llegada),
    terminado_en TIMESTAMP WITH TIME ZONE,
    terminado_por BIGINT REFERENCES usuario(id_usuario)
);

CREATE TABLE borrador_motocicleta (
    id_llegada BIGINT NOT NULL REFERENCES llegada(id_llegada),
    posicion INTEGER NOT NULL CHECK (posicion > 0),
    marca VARCHAR(120) NOT NULL,
    modelo VARCHAR(120) NOT NULL,
    color VARCHAR(120) NOT NULL,
    chasis_completo VARCHAR(120) NOT NULL,
    numero_motor VARCHAR(120) NOT NULL,
    dua VARCHAR(120) NOT NULL,
    item_dua VARCHAR(120) NOT NULL,
    guardado_por BIGINT NOT NULL REFERENCES usuario(id_usuario),
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_llegada, posicion)
);

CREATE INDEX ix_borrador_chasis ON borrador_motocicleta (upper(btrim(chasis_completo)));
