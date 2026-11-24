SELECT 'llegada:'||l.id_llegada clave, l.fecha_llegada fecha, 10 orden,
 'LLEGADA_REGISTRADA' tipo, 'Llegada registrada. Guía '||l.numero_guia descripcion,
 u.nombre_usuario registrado_por, NULL::text origen, NULL::text destino, NULL::bigint solicitud_id
FROM motocicleta m JOIN llegada l USING(id_llegada) JOIN usuario u ON u.id_usuario=l.id_receptor
WHERE m.id_motocicleta=:id
UNION ALL
SELECT 'ficha:'||m.id_motocicleta, m.creado_en,20,'FICHA_REGISTRADA','Ficha de motocicleta registrada',u.nombre_usuario,NULL,NULL,NULL
FROM motocicleta m JOIN usuario u ON u.id_usuario=m.id_creador WHERE m.id_motocicleta=:id
UNION ALL
SELECT 'movimiento:'||m.id_movimiento,m.ocurrido_en,30,m.tipo,
 CASE WHEN m.tipo='TRASLADO' THEN 'Traslado confirmado' ELSE 'Ubicación registrada' END,
 u.nombre_usuario,o.nombre,d.nombre,NULL
FROM movimiento_motocicleta m JOIN usuario u ON u.id_usuario=m.id_registrador
LEFT JOIN ubicacion o ON o.id_ubicacion=m.id_origen JOIN ubicacion d ON d.id_ubicacion=m.id_destino
WHERE m.id_motocicleta=:id
UNION ALL
SELECT 'asignacion:'||a.id_asociacion,a.asociada_en,40,'ASIGNADA','Unidad asignada a solicitud',u.nombre_usuario,NULL,NULL,d.id_solicitud
FROM solicitud_ensamblaje_unidad a JOIN solicitud_ensamblaje_detalle d USING(id_detalle)
JOIN usuario u ON u.id_usuario=a.id_asignador WHERE a.id_motocicleta=:id
UNION ALL
SELECT 'retiro:'||a.id_asociacion,a.retirada_en,50,'ASIGNACION_RETIRADA','Unidad retirada de la solicitud',u.nombre_usuario,NULL,NULL,d.id_solicitud
FROM solicitud_ensamblaje_unidad a JOIN solicitud_ensamblaje_detalle d USING(id_detalle)
JOIN usuario u ON u.id_usuario=a.id_retirador WHERE a.id_motocicleta=:id AND a.retirada_en IS NOT NULL
UNION ALL
SELECT 'seguimiento:'||e.id_evento,e.registrado_en,60,'SOLICITUD_'||e.estado,'Avance de solicitud: '||e.estado,u.nombre_usuario,NULL,NULL,e.id_solicitud
FROM solicitud_ensamblaje_evento e JOIN usuario u ON u.id_usuario=e.id_registrador
WHERE EXISTS(SELECT 1 FROM solicitud_ensamblaje_unidad a JOIN solicitud_ensamblaje_detalle d USING(id_detalle)
 WHERE a.id_motocicleta=:id AND d.id_solicitud=e.id_solicitud AND e.registrado_en>=a.asociada_en
 AND (a.retirada_en IS NULL OR e.registrado_en<a.retirada_en))
