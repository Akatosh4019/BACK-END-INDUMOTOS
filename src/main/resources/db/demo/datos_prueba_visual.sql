-- Datos de demostración para una base local. Ejecutar manualmente; nunca como migración.
-- Repetir este archivo no duplica las diez motos ni sus ubicaciones iniciales.
BEGIN;

INSERT INTO ubicacion (codigo, nombre, tipo, referencia)
SELECT v.codigo, v.nombre, v.tipo, v.referencia
FROM (VALUES
 ('DEMO_ALM_CENTRAL','Almacén central de prueba','ALMACEN','Lima'),
 ('DEMO_TIENDA_NORTE','Tienda norte de prueba','TIENDA','Lima'),
 ('DEMO_TALLER','Taller de prueba','TALLER','Lima')
) AS v(codigo,nombre,tipo,referencia)
WHERE NOT EXISTS (SELECT 1 FROM ubicacion u WHERE upper(btrim(u.codigo))=upper(btrim(v.codigo)));

INSERT INTO llegada (numero_guia, id_receptor, cantidad_declarada, cantidad_recibida, observacion)
SELECT 'GR-DEMO-VISUAL-10', u.id_usuario, 10, 10, 'Diez unidades para pruebas visuales del Sprint 2'
FROM usuario u WHERE u.nombre_usuario = current_setting('indumotos.demo_admin')
AND NOT EXISTS (SELECT 1 FROM llegada l WHERE l.numero_guia='GR-DEMO-VISUAL-10');

INSERT INTO motocicleta (id_llegada,marca,modelo,color,chasis_completo,numero_motor,dua,item_dua,id_creador)
SELECT l.id_llegada,v.marca,v.modelo,v.color,v.chasis,v.motor,'DUA-DEMO-2026',v.item,u.id_usuario
FROM (VALUES
 ('Honda','CB 190R','Rojo','IND-DEMO-S2-0001','M-DEMO-0001','01'),
 ('Honda','CB 190R','Azul','IND-DEMO-S2-0002','M-DEMO-0002','02'),
 ('Yamaha','FZ 25','Negro','IND-DEMO-S2-0003','M-DEMO-0003','03'),
 ('Yamaha','FZ 25','Blanco','IND-DEMO-S2-0004','M-DEMO-0004','04'),
 ('Bajaj','Pulsar NS 200','Azul','IND-DEMO-S2-0005','M-DEMO-0005','05'),
 ('Bajaj','Pulsar NS 200','Rojo','IND-DEMO-S2-0006','M-DEMO-0006','06'),
 ('Suzuki','Gixxer 150','Gris','IND-DEMO-S2-0007','M-DEMO-0007','07'),
 ('Suzuki','Gixxer 150','Negro','IND-DEMO-S2-0008','M-DEMO-0008','08'),
 ('TVS','Apache RTR 200','Blanco','IND-DEMO-S2-0009','M-DEMO-0009','09'),
 ('TVS','Apache RTR 200','Amarillo','IND-DEMO-S2-0010','M-DEMO-0010','10')
) AS v(marca,modelo,color,chasis,motor,item)
CROSS JOIN usuario u CROSS JOIN llegada l
WHERE u.nombre_usuario=current_setting('indumotos.demo_admin') AND l.numero_guia='GR-DEMO-VISUAL-10'
AND NOT EXISTS (SELECT 1 FROM motocicleta m WHERE upper(btrim(m.chasis_completo))=upper(btrim(v.chasis)));

INSERT INTO movimiento_motocicleta (id_motocicleta,tipo,id_destino,id_registrador,ocurrido_en,observacion)
SELECT m.id_motocicleta,'UBICACION',d.id_ubicacion,u.id_usuario,CURRENT_TIMESTAMP,'Ubicación inicial para demostración'
FROM motocicleta m CROSS JOIN ubicacion d CROSS JOIN usuario u
WHERE m.chasis_completo LIKE 'IND-DEMO-S2-%' AND d.codigo='DEMO_ALM_CENTRAL'
AND u.nombre_usuario=current_setting('indumotos.demo_admin')
AND NOT EXISTS (SELECT 1 FROM movimiento_motocicleta mm WHERE mm.id_motocicleta=m.id_motocicleta);

COMMIT;

SELECT count(*) AS motocicletas_demo FROM motocicleta WHERE chasis_completo LIKE 'IND-DEMO-S2-%';
