-- HU-05: separar la consulta de ubicaciones del registro de cambios.
-- Mantener las excepciones individuales y las reglas de Sprint 1.
INSERT INTO permiso (codigo, nombre, descripcion) VALUES
('UBICACIONES_CONSULTAR', 'Consultar ubicaciones', 'HU-05: consultar ubicación y recorrido'),
('UBICACIONES_REGISTRAR', 'Registrar ubicación', 'HU-05: registrar ubicación conocida')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM (VALUES
    ('ADMIN', 'UBICACIONES_CONSULTAR'),
    ('ADMIN', 'UBICACIONES_REGISTRAR'),
    ('ALMACEN', 'UBICACIONES_CONSULTAR'),
    ('ALMACEN', 'UBICACIONES_REGISTRAR'),
    ('INVENTARIOS', 'UBICACIONES_CONSULTAR'),
    ('INVENTARIOS', 'UBICACIONES_REGISTRAR')
) AS asignacion(rol_codigo, permiso_codigo)
JOIN rol r ON r.codigo = asignacion.rol_codigo
JOIN permiso p ON p.codigo = asignacion.permiso_codigo
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
