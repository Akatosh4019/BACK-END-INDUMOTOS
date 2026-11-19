INSERT INTO permiso(codigo,nombre,descripcion) VALUES
('SOLICITUDES_AVANZAR','Actualizar solicitudes','HU-07: avance y resultado por Logística')
ON CONFLICT(codigo) DO NOTHING;

INSERT INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.codigo IN ('ADMIN','ENSAMBLAJE') AND p.codigo='SOLICITUDES_AVANZAR'
ON CONFLICT(id_rol,id_permiso) DO NOTHING;
