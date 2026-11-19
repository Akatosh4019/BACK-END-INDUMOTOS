INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r CROSS JOIN permiso p
WHERE r.codigo = 'ALMACEN'
  AND p.codigo IN ('SOLICITUDES_CONSULTAR', 'SOLICITUDES_AVANZAR', 'UNIDADES_ASIGNAR')
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
