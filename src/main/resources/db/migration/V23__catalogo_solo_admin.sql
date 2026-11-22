DELETE FROM rol_permiso rp
USING rol r, permiso p
WHERE rp.id_rol=r.id_rol AND rp.id_permiso=p.id_permiso
  AND p.codigo='CATALOGO_GESTIONAR' AND r.codigo<>'ADMIN';

DELETE FROM usuario_permiso up
USING usuario u, rol r, permiso p
WHERE up.id_usuario=u.id_usuario AND u.id_rol=r.id_rol
  AND up.id_permiso=p.id_permiso AND p.codigo='CATALOGO_GESTIONAR'
  AND r.codigo<>'ADMIN' AND up.decision='PERMITIR';
