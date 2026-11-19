-- Ejecutar solamente en una base de pruebas con V1-V15; revierte todos los datos de ensayo.
BEGIN;

INSERT INTO usuario(id_rol,nombre_completo,nombre_usuario,clave_hash)
SELECT id_rol,'Verificacion Sprint 2','s2_sql_verificacion','hash_no_utilizable'
FROM rol WHERE codigo='ADMIN';

INSERT INTO ubicacion(codigo,nombre,tipo) VALUES
('S2_SQL_ALM','Almacen de ensayo','ALMACEN'),
('S2_SQL_TIE','Tienda de ensayo','TIENDA'),
('S2_SQL_DES','Despacho de ensayo','DESPACHO');

INSERT INTO llegada(numero_guia,id_receptor,cantidad_declarada,cantidad_recibida)
SELECT 'S2_SQL_GUIA',id_usuario,1,1 FROM usuario WHERE nombre_usuario='s2_sql_verificacion';

INSERT INTO motocicleta(id_llegada,marca,modelo,color,chasis_completo,numero_motor,dua,item_dua,id_creador)
SELECT l.id_llegada,'Marca ensayo','Modelo ensayo','Rojo','S2_SQL_CHASIS','S2_SQL_MOTOR',
    'S2_SQL_DUA','S2_SQL_ITEM',u.id_usuario FROM llegada l CROSS JOIN usuario u
WHERE l.numero_guia='S2_SQL_GUIA' AND u.nombre_usuario='s2_sql_verificacion';

INSERT INTO movimiento_motocicleta(id_motocicleta,tipo,id_destino,id_registrador,ocurrido_en)
SELECT m.id_motocicleta,'UBICACION',d.id_ubicacion,u.id_usuario,CURRENT_TIMESTAMP
FROM motocicleta m CROSS JOIN ubicacion d CROSS JOIN usuario u
WHERE m.chasis_completo='S2_SQL_CHASIS' AND d.codigo='S2_SQL_ALM'
AND u.nombre_usuario='s2_sql_verificacion';

INSERT INTO movimiento_motocicleta(id_motocicleta,tipo,id_origen,id_destino,id_registrador,ocurrido_en)
SELECT m.id_motocicleta,'TRASLADO',o.id_ubicacion,d.id_ubicacion,u.id_usuario,CURRENT_TIMESTAMP
FROM motocicleta m CROSS JOIN ubicacion o CROSS JOIN ubicacion d CROSS JOIN usuario u
WHERE m.chasis_completo='S2_SQL_CHASIS' AND o.codigo='S2_SQL_ALM'
AND d.codigo='S2_SQL_TIE' AND u.nombre_usuario='s2_sql_verificacion';

INSERT INTO solicitud_ensamblaje(id_solicitante,id_destino)
SELECT u.id_usuario,d.id_ubicacion FROM usuario u CROSS JOIN ubicacion d
WHERE u.nombre_usuario='s2_sql_verificacion' AND d.codigo='S2_SQL_TIE';

INSERT INTO solicitud_ensamblaje_detalle(id_solicitud,marca,modelo,color,cantidad)
SELECT s.id_solicitud,'Marca ensayo','Modelo ensayo','Rojo',1
FROM solicitud_ensamblaje s JOIN usuario u ON u.id_usuario=s.id_solicitante
WHERE u.nombre_usuario='s2_sql_verificacion';

INSERT INTO solicitud_ensamblaje_evento(id_solicitud,estado,id_registrador)
SELECT s.id_solicitud,'PENDIENTE',u.id_usuario
FROM solicitud_ensamblaje s JOIN usuario u ON u.id_usuario=s.id_solicitante
WHERE u.nombre_usuario='s2_sql_verificacion';

INSERT INTO solicitud_ensamblaje_evento(id_solicitud,estado,id_registrador)
SELECT s.id_solicitud,'EN_PREPARACION',u.id_usuario
FROM solicitud_ensamblaje s JOIN usuario u ON u.id_usuario=s.id_solicitante
WHERE u.nombre_usuario='s2_sql_verificacion';

INSERT INTO solicitud_ensamblaje_unidad(id_detalle,id_motocicleta,id_asignador)
SELECT d.id_detalle,m.id_motocicleta,u.id_usuario FROM solicitud_ensamblaje_detalle d
JOIN solicitud_ensamblaje s ON s.id_solicitud=d.id_solicitud
JOIN usuario u ON u.id_usuario=s.id_solicitante CROSS JOIN motocicleta m
WHERE u.nombre_usuario='s2_sql_verificacion' AND m.chasis_completo='S2_SQL_CHASIS';

DO $verify$
BEGIN
    IF (SELECT count(*) FROM movimiento_motocicleta mm JOIN motocicleta m
        ON m.id_motocicleta=mm.id_motocicleta WHERE m.chasis_completo='S2_SQL_CHASIS') <> 2
       OR NOT EXISTS(SELECT 1 FROM motocicleta_ubicacion_actual a
        JOIN motocicleta m ON m.id_motocicleta=a.id_motocicleta
        JOIN ubicacion d ON d.id_ubicacion=a.id_ubicacion
        WHERE m.chasis_completo='S2_SQL_CHASIS' AND d.codigo='S2_SQL_TIE')
       OR NOT EXISTS(SELECT 1 FROM solicitud_ensamblaje_estado_actual e
        JOIN solicitud_ensamblaje s ON s.id_solicitud=e.id_solicitud
        JOIN usuario u ON u.id_usuario=s.id_solicitante
        WHERE u.nombre_usuario='s2_sql_verificacion' AND e.estado='EN_PREPARACION')
       OR NOT EXISTS(SELECT 1 FROM solicitud_ensamblaje_unidad a
        JOIN motocicleta m ON m.id_motocicleta=a.id_motocicleta
        WHERE m.chasis_completo='S2_SQL_CHASIS' AND a.retirada_en IS NULL)
    THEN RAISE EXCEPTION 'La integracion SQL del Sprint 2 no conserva la ubicacion, estado y asignacion';
    END IF;
END $verify$;

ROLLBACK;
