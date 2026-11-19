INSERT INTO permiso(codigo,nombre,descripcion) VALUES
('SOLICITUDES_CREAR','Crear solicitudes','HU-06: envío de solicitudes comerciales'),
('SOLICITUDES_CONSULTAR','Consultar solicitudes','HU-06: consulta de solicitudes y líneas')
ON CONFLICT(codigo) DO NOTHING;

INSERT INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM (VALUES
('ADMIN','SOLICITUDES_CREAR'),('ADMIN','SOLICITUDES_CONSULTAR'),
('COMERCIAL','SOLICITUDES_CREAR'),('COMERCIAL','SOLICITUDES_CONSULTAR'),
('ENSAMBLAJE','SOLICITUDES_CONSULTAR')) AS a(rol,permiso)
JOIN rol r ON r.codigo=a.rol JOIN permiso p ON p.codigo=a.permiso
ON CONFLICT(id_rol,id_permiso) DO NOTHING;

-- Comercial necesita consultar sedes para elegir el destino de la solicitud.
INSERT INTO rol_permiso(id_rol,id_permiso)
SELECT r.id_rol,p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.codigo='COMERCIAL' AND p.codigo='UBICACIONES_CONSULTAR'
ON CONFLICT(id_rol,id_permiso) DO NOTHING;
