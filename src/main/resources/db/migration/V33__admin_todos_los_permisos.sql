INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r CROSS JOIN permiso p
WHERE r.codigo = 'ADMIN'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

DELETE FROM usuario_permiso up
USING usuario u JOIN rol r ON r.id_rol = u.id_rol
WHERE up.id_usuario = u.id_usuario
  AND r.codigo = 'ADMIN'
  AND up.decision = 'DENEGAR';
