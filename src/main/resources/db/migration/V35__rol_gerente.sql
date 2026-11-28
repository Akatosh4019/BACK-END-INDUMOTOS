-- Gerencia toma las decisiones de negocio; Administración solo gestiona personal y accesos.
INSERT INTO rol (codigo, nombre, descripcion) VALUES
('GERENTE', 'Gerencia', 'Decisiones de negocio, catálogo, reportes y seguimiento general')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r CROSS JOIN permiso p
WHERE r.codigo = 'GERENTE'
  AND p.codigo IN ('INVENTARIO_CONSULTAR', 'UBICACIONES_CONSULTAR', 'SOLICITUDES_CONSULTAR',
    'SOLICITUDES_ANULAR', 'DISPONIBILIDAD_CONSULTAR', 'DIFERENCIAS_CONSULTAR', 'TIENDA_CONSULTAR',
    'COMPANIA_CONSULTAR', 'CATALOGO_GESTIONAR', 'REPORTES_CONSULTAR', 'PAGOS_CONFIRMAR')
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- Administración conserva únicamente la gestión del personal.
DELETE FROM rol_permiso rp
USING rol r, permiso p
WHERE rp.id_rol = r.id_rol AND rp.id_permiso = p.id_permiso
  AND r.codigo = 'ADMIN'
  AND p.codigo NOT IN ('USUARIOS_CREAR', 'USUARIOS_CONSULTAR', 'USUARIOS_DESACTIVAR', 'PERMISOS_ASIGNAR');

-- Las excepciones que daban permisos operativos a cuentas de Administración dejan de aplicarse.
DELETE FROM usuario_permiso up
USING usuario u, rol r, permiso p
WHERE up.id_usuario = u.id_usuario AND u.id_rol = r.id_rol AND up.id_permiso = p.id_permiso
  AND r.codigo = 'ADMIN' AND up.decision = 'PERMITIR'
  AND p.codigo NOT IN ('USUARIOS_CREAR', 'USUARIOS_CONSULTAR', 'USUARIOS_DESACTIVAR', 'PERMISOS_ASIGNAR');
