INSERT INTO permiso (codigo, nombre, descripcion)
VALUES ('FICHAS_EDITAR', 'Editar fichas', 'Editar datos de motocicletas existentes')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r CROSS JOIN permiso p
WHERE r.codigo IN ('ADMIN', 'INVENTARIOS') AND p.codigo = 'FICHAS_EDITAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
