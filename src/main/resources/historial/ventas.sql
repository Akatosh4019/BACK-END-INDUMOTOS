SELECT 'venta:'||v.id_venta clave,v.vendida_en fecha,90 orden,'VENTA_REGISTRADA' tipo,'Venta registrada por Facturación' descripcion,
 u.nombre_usuario registrado_por,NULL::text origen,d.nombre destino,NULL::bigint solicitud_id
FROM venta_motocicleta v JOIN usuario u ON u.id_usuario=v.%s JOIN ubicacion d ON d.id_ubicacion=v.id_tienda
WHERE v.id_motocicleta=:id
