INSERT INTO permiso(codigo,nombre,descripcion) VALUES
('UNIDADES_ASIGNAR','Asociar motocicletas','HU-08: asociar y retirar unidades de solicitudes')
ON CONFLICT(codigo) DO NOTHING;

INSERT INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.codigo IN ('ADMIN','ENSAMBLAJE') AND p.codigo='UNIDADES_ASIGNAR'
ON CONFLICT(id_rol,id_permiso) DO NOTHING;
