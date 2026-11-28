CREATE TABLE ocr_lectura_guia (
    id_llegada BIGINT PRIMARY KEY REFERENCES llegada(id_llegada),
    fotos_analizadas INTEGER NOT NULL CHECK (fotos_analizadas BETWEEN 1 AND 3),
    analizada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE ocr_sugerencia_guia (
    id_llegada BIGINT NOT NULL REFERENCES ocr_lectura_guia(id_llegada) ON DELETE CASCADE,
    posicion INTEGER NOT NULL CHECK (posicion > 0),
    pagina INTEGER NOT NULL CHECK (pagina BETWEEN 1 AND 3),
    marca VARCHAR(120) NOT NULL,
    modelo VARCHAR(120) NOT NULL,
    color VARCHAR(120) NOT NULL,
    chasis_completo VARCHAR(120) NOT NULL,
    numero_motor VARCHAR(120) NOT NULL,
    dua VARCHAR(120) NOT NULL,
    item_dua VARCHAR(120) NOT NULL,
    PRIMARY KEY(id_llegada, posicion)
);
