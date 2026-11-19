ALTER TABLE ubicacion DROP CONSTRAINT ck_ubicacion_tipo;
ALTER TABLE ubicacion ADD CONSTRAINT ck_ubicacion_tipo
    CHECK (tipo IN ('ALMACEN','TIENDA','TALLER','DESPACHO'));

INSERT INTO permiso(codigo,nombre,descripcion) VALUES
('TRASLADOS_REGISTRAR','Registrar traslados','HU-09: traslado de motocicletas')
ON CONFLICT(codigo) DO NOTHING;

INSERT INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.codigo IN ('ADMIN','ALMACEN') AND p.codigo='TRASLADOS_REGISTRAR'
ON CONFLICT(id_rol,id_permiso) DO NOTHING;
