INSERT INTO permiso(codigo,nombre,descripcion) VALUES
('SOLICITUDES_ANULAR','Anular solicitudes','HU-27: anular solicitudes pendientes con motivo')
ON CONFLICT(codigo) DO NOTHING;

INSERT INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.codigo IN ('ADMIN','COMERCIAL') AND p.codigo='SOLICITUDES_ANULAR'
ON CONFLICT(id_rol,id_permiso) DO NOTHING;
