SELECT 'envio:'||e.id_envio clave,e.despachado_en fecha,70 orden,'DESPACHADA' tipo,'Salida registrada' descripcion,
 u.nombre_usuario registrado_por,o.nombre origen,d.nombre destino,e.id_solicitud solicitud_id
FROM envio_solicitud_motocicleta e JOIN usuario u ON u.id_usuario=e.despachado_por
JOIN ubicacion o ON o.id_ubicacion=e.id_origen JOIN ubicacion d ON d.id_ubicacion=e.id_destino WHERE e.id_motocicleta=:id
UNION ALL
SELECT 'recepcion:'||e.id_envio,e.recibido_en,80,'RECEPCION_CONFIRMADA','Recepción física confirmada',u.nombre_usuario,o.nombre,d.nombre,e.id_solicitud
FROM envio_solicitud_motocicleta e JOIN usuario u ON u.id_usuario=e.recibido_por
JOIN ubicacion o ON o.id_ubicacion=e.id_origen JOIN ubicacion d ON d.id_ubicacion=e.id_destino
WHERE e.id_motocicleta=:id AND e.recibido_en IS NOT NULL
