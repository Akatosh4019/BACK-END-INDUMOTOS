-- Una guía de remisión y un número de motor identifican un único registro.
-- Se comparan sin espacios exteriores ni diferencias de mayúsculas.
CREATE UNIQUE INDEX ux_llegada_numero_guia ON llegada (upper(btrim(numero_guia)));
CREATE UNIQUE INDEX ux_motocicleta_numero_motor ON motocicleta (upper(btrim(numero_motor)));
