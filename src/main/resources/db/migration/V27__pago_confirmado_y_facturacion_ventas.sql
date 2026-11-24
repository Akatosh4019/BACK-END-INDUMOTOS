-- V26 ya se aplicó: ampliar con nueva migración para conservar su checksum.
ALTER TABLE venta_motocicleta ADD COLUMN referencia_pago VARCHAR(100);
ALTER TABLE venta_motocicleta ADD CONSTRAINT ck_venta_pago
    CHECK (referencia_pago IS NULL OR btrim(referencia_pago) <> '');

DELETE FROM rol_permiso rp USING rol r,permiso p
WHERE rp.id_rol=r.id_rol AND rp.id_permiso=p.id_permiso
AND r.codigo='COMERCIAL' AND p.codigo='VENTAS_REGISTRAR';
INSERT INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.codigo='FACTURACION' AND p.codigo='VENTAS_REGISTRAR'
ON CONFLICT(id_rol,id_permiso) DO NOTHING;
