ALTER TABLE llegada ADD COLUMN estado_registro VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE llegada ADD CONSTRAINT ck_llegada_estado_registro
    CHECK (estado_registro IN ('PENDIENTE', 'CONFIRMADA'));

UPDATE llegada l SET estado_registro='CONFIRMADA'
WHERE EXISTS (SELECT 1 FROM registro_guia r
    WHERE r.id_llegada=l.id_llegada AND r.terminado_en IS NOT NULL);

CREATE INDEX ix_llegada_estado_fecha ON llegada
    (estado_registro, fecha_llegada DESC, id_llegada DESC)
    WHERE visible_en_listado;
