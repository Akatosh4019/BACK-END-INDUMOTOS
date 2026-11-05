INSERT INTO permiso (codigo, nombre, descripcion) VALUES
('USUARIOS_CREAR', 'Crear cuentas', 'HU-14'),
('PERMISOS_ASIGNAR', 'Administrar accesos', 'HU-15'),
('LLEGADAS_REGISTRAR', 'Registrar llegadas', 'HU-04'),
('FICHAS_REGISTRAR', 'Registrar fichas', 'HU-02'),
('INVENTARIO_CONSULTAR', 'Consultar inventario', 'HU-01 y HU-03')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM (VALUES
    ('ADMIN', 'USUARIOS_CREAR'),
    ('ADMIN', 'PERMISOS_ASIGNAR'),
    ('ADMIN', 'INVENTARIO_CONSULTAR'),
    ('ALMACEN', 'LLEGADAS_REGISTRAR'),
    ('ALMACEN', 'INVENTARIO_CONSULTAR'),
    ('INVENTARIOS', 'FICHAS_REGISTRAR'),
    ('INVENTARIOS', 'INVENTARIO_CONSULTAR')
) AS asignacion(rol_codigo, permiso_codigo)
JOIN rol r ON r.codigo = asignacion.rol_codigo
JOIN permiso p ON p.codigo = asignacion.permiso_codigo
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
