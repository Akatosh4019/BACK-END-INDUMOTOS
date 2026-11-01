BEGIN;

INSERT INTO rol (codigo, nombre) VALUES ('PRUEBA_ROL_TEMP', 'Rol de prueba');
INSERT INTO permiso (codigo, nombre) VALUES ('PRUEBA_PERMISO_TEMP', 'Permiso de prueba');
INSERT INTO usuario (id_rol, nombre_completo, nombre_usuario, clave_hash)
SELECT id_rol, 'Usuario de prueba', 'prueba_usuario_temp', 'hash_solo_para_prueba'
FROM rol WHERE codigo = 'PRUEBA_ROL_TEMP';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM rol r CROSS JOIN permiso p
WHERE r.codigo = 'PRUEBA_ROL_TEMP' AND p.codigo = 'PRUEBA_PERMISO_TEMP';

INSERT INTO usuario_permiso (id_usuario, id_permiso, decision)
SELECT u.id_usuario, p.id_permiso, 'DENEGAR' FROM usuario u CROSS JOIN permiso p
WHERE u.nombre_usuario = 'prueba_usuario_temp' AND p.codigo = 'PRUEBA_PERMISO_TEMP';

DO $$
DECLARE
    rol_id BIGINT;
    permiso_id BIGINT;
    usuario_id BIGINT;
BEGIN
    SELECT id_rol INTO rol_id FROM rol WHERE codigo = 'PRUEBA_ROL_TEMP';
    SELECT id_permiso INTO permiso_id FROM permiso WHERE codigo = 'PRUEBA_PERMISO_TEMP';
    SELECT id_usuario INTO usuario_id FROM usuario WHERE nombre_usuario = 'prueba_usuario_temp';

    BEGIN
        INSERT INTO rol_permiso (id_rol, id_permiso) VALUES (rol_id, permiso_id);
        RAISE EXCEPTION 'No se rechazó rol_permiso duplicado';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO usuario_permiso (id_usuario, id_permiso, decision)
        VALUES (usuario_id, permiso_id, 'PERMITIR');
        RAISE EXCEPTION 'No se rechazó usuario_permiso duplicado';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO usuario (id_rol, nombre_completo, nombre_usuario, clave_hash)
        VALUES (rol_id, 'Duplicado', 'prueba_usuario_temp', 'hash_solo_para_prueba');
        RAISE EXCEPTION 'No se rechazó nombre_usuario duplicado';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;

    BEGIN
        INSERT INTO usuario (id_rol, nombre_completo, nombre_usuario, clave_hash)
        VALUES (-1, 'Sin rol', 'prueba_sin_rol_temp', 'hash_solo_para_prueba');
        RAISE EXCEPTION 'No se rechazó id_rol inexistente';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
END $$;

ROLLBACK;
