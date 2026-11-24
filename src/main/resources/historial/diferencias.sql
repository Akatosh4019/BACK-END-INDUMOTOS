SELECT 'diferencia:'||d.id_diferencia clave,d.reportada_en fecha,110 orden,'DIFERENCIA_REPORTADA' tipo,
 'Diferencia reportada en '||d.campo descripcion,u.nombre_usuario registrado_por,NULL::text origen,NULL::text destino,NULL::bigint solicitud_id
FROM diferencia_datos d JOIN usuario u ON u.id_usuario=d.id_reportante WHERE d.id_motocicleta=:id
UNION ALL
SELECT 'resolucion:'||d.id_diferencia,d.resuelta_en,120,'DIFERENCIA_RESUELTA','Diferencia resuelta en '||d.campo,u.nombre_usuario,NULL,NULL,NULL
FROM diferencia_datos d JOIN usuario u ON u.id_usuario=d.id_resolutor WHERE d.id_motocicleta=:id AND d.resuelta_en IS NOT NULL
