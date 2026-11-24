-- El área de Facturación registra el hecho; quien vende al cliente puede ser otra persona.
ALTER TABLE venta_motocicleta RENAME COLUMN id_vendedor TO id_registrador;
