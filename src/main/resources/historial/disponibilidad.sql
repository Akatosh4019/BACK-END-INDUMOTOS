SELECT 'disponibilidad:'||e.id_evento clave,e.registrado_en fecha,100 orden,'DISPONIBILIDAD_'||e.estado tipo,
 'Disponibilidad actualizada: '||e.estado descripcion,u.nombre_usuario registrado_por,NULL::text origen,NULL::text destino,NULL::bigint solicitud_id
FROM disponibilidad_evento e JOIN usuario u ON u.id_usuario=e.id_registrador WHERE e.id_motocicleta=:id
