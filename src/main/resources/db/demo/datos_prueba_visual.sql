-- Datos de demostración para una base local. Se cargan solo con el perfil demo.
-- Repetir este archivo no duplica las motos, guías ni ubicaciones iniciales.
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
SELECT 'GR-DEMO-VISUAL-10', u.id_usuario, 20, 20, 'Veinte unidades para pruebas visuales del Sprint 2'
FROM usuario u WHERE u.nombre_usuario = current_setting('indumotos.demo_admin')
AND NOT EXISTS (SELECT 1 FROM llegada l WHERE l.numero_guia='GR-DEMO-VISUAL-10');

UPDATE llegada SET cantidad_declarada=20,cantidad_recibida=20
WHERE numero_guia='GR-DEMO-VISUAL-10' AND observacion LIKE '%pruebas visuales del Sprint 2%';

-- Dos guías completamente ficticias para practicar el registro sin datos de terceros.
INSERT INTO llegada (numero_guia,id_receptor,cantidad_declarada,cantidad_recibida,observacion)
SELECT v.numero,u.id_usuario,v.cantidad,v.cantidad,
    'Guía ficticia para pruebas de software; fecha_llegada indica la carga de la demo'
FROM (VALUES ('GUIA-DEMO-001',2),('GUIA-DEMO-002',3)) v(numero,cantidad)
CROSS JOIN usuario u
WHERE u.nombre_usuario=current_setting('indumotos.demo_admin')
AND NOT EXISTS (SELECT 1 FROM llegada l WHERE l.numero_guia=v.numero);

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
 ('TVS','Apache RTR 200','Amarillo','IND-DEMO-S2-0010','M-DEMO-0010','10'),
 ('Honda','CB 190R','Negro','IND-DEMO-S2-0011','M-DEMO-0011','11'),
 ('Yamaha','FZ 25','Azul','IND-DEMO-S2-0012','M-DEMO-0012','12'),
 ('Bajaj','Pulsar NS 200','Blanco','IND-DEMO-S2-0013','M-DEMO-0013','13'),
 ('Suzuki','Gixxer 150','Rojo','IND-DEMO-S2-0014','M-DEMO-0014','14'),
 ('TVS','Apache RTR 200','Gris','IND-DEMO-S2-0015','M-DEMO-0015','15'),
 ('Honda','XR 150L','Rojo','IND-DEMO-S2-0016','M-DEMO-0016','16'),
 ('Honda','XR 150L','Blanco','IND-DEMO-S2-0017','M-DEMO-0017','17'),
 ('Yamaha','XTZ 125','Negro','IND-DEMO-S2-0018','M-DEMO-0018','18'),
 ('Bajaj','Pulsar N 250','Azul','IND-DEMO-S2-0019','M-DEMO-0019','19'),
 ('Suzuki','Gixxer 250','Amarillo','IND-DEMO-S2-0020','M-DEMO-0020','20')
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

-- Las 20 fichas de prueba ya existen: cerrar su guía evita mostrarla como incompleta.
INSERT INTO registro_guia(id_llegada,terminado_en,terminado_por)
SELECT l.id_llegada,CURRENT_TIMESTAMP,u.id_usuario FROM llegada l CROSS JOIN usuario u
WHERE l.numero_guia='GR-DEMO-VISUAL-10'
AND l.observacion LIKE '%pruebas visuales del Sprint 2%'
AND u.nombre_usuario=current_setting('indumotos.demo_admin')
AND (SELECT count(*) FROM motocicleta m WHERE m.id_llegada=l.id_llegada)=20
ON CONFLICT (id_llegada) DO NOTHING;

UPDATE llegada l SET estado_registro='CONFIRMADA'
WHERE l.numero_guia='GR-DEMO-VISUAL-10'
AND EXISTS (SELECT 1 FROM registro_guia r WHERE r.id_llegada=l.id_llegada AND r.terminado_en IS NOT NULL);

-- El catálogo se crea tras Flyway V18; los datos demo deben cargarse por separado.
INSERT INTO catalogo_motocicleta(marca,modelo,color,id_creador)
SELECT DISTINCT m.marca,m.modelo,m.color,u.id_usuario FROM motocicleta m CROSS JOIN usuario u
WHERE m.chasis_completo LIKE 'IND-DEMO-S2-%'
AND u.nombre_usuario=current_setting('indumotos.demo_admin')
ON CONFLICT DO NOTHING;

INSERT INTO catalogo_motocicleta(marca,modelo,color,id_creador)
SELECT v.marca,v.modelo,v.color,u.id_usuario FROM (VALUES
 ('DEMO','Urbana 150','Azul'),
 ('DEMO','Urbana 150','Rojo'),
 ('DEMO','Ruta 250','Negro'),
 ('DEMO','Ruta 250','Gris'),
 ('DEMO','Ruta 250','Azul')
) v(marca,modelo,color) CROSS JOIN usuario u
WHERE u.nombre_usuario=current_setting('indumotos.demo_admin')
ON CONFLICT DO NOTHING;

-- Ventas ficticias para que el módulo de Reportes muestre datos en la demostración.
-- Usan motos propias (IND-DEMO-V-*) para no tocar las 20 motos de prueba anteriores.
INSERT INTO llegada (numero_guia,fecha_llegada,id_receptor,cantidad_declarada,cantidad_recibida,observacion,estado_registro)
SELECT 'GR-DEMO-VENTAS', CURRENT_TIMESTAMP - interval '410 days', u.id_usuario, 8, 8, 'Guía ficticia con motos ya vendidas para el reporte de ventas', 'CONFIRMADA'
FROM usuario u WHERE u.nombre_usuario=current_setting('indumotos.demo_admin')
AND NOT EXISTS (SELECT 1 FROM llegada l WHERE l.numero_guia='GR-DEMO-VENTAS');

INSERT INTO motocicleta (id_llegada,marca,modelo,color,chasis_completo,numero_motor,dua,item_dua,id_creador,creado_en)
SELECT l.id_llegada,v.marca,v.modelo,v.color,v.chasis,v.motor,'DUA-DEMO-2026',v.item,u.id_usuario,
    CURRENT_TIMESTAMP - interval '405 days'
FROM (VALUES
 ('Honda','CB 190R','Rojo','IND-DEMO-V-0001','M-DEMO-V-0001','V1'),
 ('Honda','CB 190R','Negro','IND-DEMO-V-0002','M-DEMO-V-0002','V2'),
 ('Honda','CB 190R','Azul','IND-DEMO-V-0003','M-DEMO-V-0003','V3'),
 ('Yamaha','FZ 25','Negro','IND-DEMO-V-0004','M-DEMO-V-0004','V4'),
 ('Yamaha','FZ 25','Azul','IND-DEMO-V-0005','M-DEMO-V-0005','V5'),
 ('Bajaj','Pulsar NS 200','Rojo','IND-DEMO-V-0006','M-DEMO-V-0006','V6'),
 ('Suzuki','Gixxer 150','Gris','IND-DEMO-V-0007','M-DEMO-V-0007','V7'),
 ('Honda','CB 190R','Rojo','IND-DEMO-V-0008','M-DEMO-V-0008','V8')
) AS v(marca,modelo,color,chasis,motor,item)
CROSS JOIN usuario u CROSS JOIN llegada l
WHERE u.nombre_usuario=current_setting('indumotos.demo_admin') AND l.numero_guia='GR-DEMO-VENTAS'
AND NOT EXISTS (SELECT 1 FROM motocicleta m WHERE upper(btrim(m.chasis_completo))=upper(btrim(v.chasis)));

INSERT INTO registro_guia(id_llegada,terminado_en,terminado_por)
SELECT l.id_llegada,CURRENT_TIMESTAMP,u.id_usuario FROM llegada l CROSS JOIN usuario u
WHERE l.numero_guia='GR-DEMO-VENTAS' AND u.nombre_usuario=current_setting('indumotos.demo_admin')
ON CONFLICT (id_llegada) DO NOTHING;

INSERT INTO movimiento_motocicleta (id_motocicleta,tipo,id_destino,id_registrador,ocurrido_en,observacion)
SELECT m.id_motocicleta,'UBICACION',d.id_ubicacion,u.id_usuario,CURRENT_TIMESTAMP - interval '400 days',
    'Ubicación de prueba en tienda antes de la venta'
FROM motocicleta m CROSS JOIN ubicacion d CROSS JOIN usuario u
WHERE m.chasis_completo LIKE 'IND-DEMO-V-%' AND d.codigo='DEMO_TIENDA_NORTE'
AND u.nombre_usuario=current_setting('indumotos.demo_admin')
AND NOT EXISTS (SELECT 1 FROM movimiento_motocicleta mm WHERE mm.id_motocicleta=m.id_motocicleta);

INSERT INTO venta_motocicleta (id_motocicleta,id_tienda,id_registrador,referencia_comprobante,importe,vendida_en)
SELECT m.id_motocicleta,d.id_ubicacion,u.id_usuario,'DEMO-B001-' || right(m.chasis_completo,4),v.importe,
    CURRENT_TIMESTAMP - v.hace
FROM (VALUES
 ('IND-DEMO-V-0001', 9800.00, interval '1 day'),
 ('IND-DEMO-V-0002', 9800.00, interval '3 days'),
 ('IND-DEMO-V-0003', 9650.00, interval '12 days'),
 ('IND-DEMO-V-0004', 14500.00, interval '5 days'),
 ('IND-DEMO-V-0005', 14500.00, interval '45 days'),
 ('IND-DEMO-V-0006', 11200.00, interval '20 days'),
 ('IND-DEMO-V-0007', 7900.00, interval '150 days'),
 ('IND-DEMO-V-0008', 9800.00, interval '240 days')
) AS v(chasis,importe,hace)
JOIN motocicleta m ON m.chasis_completo=v.chasis
CROSS JOIN ubicacion d CROSS JOIN usuario u
WHERE d.codigo='DEMO_TIENDA_NORTE' AND u.nombre_usuario=current_setting('indumotos.demo_admin')
AND NOT EXISTS (SELECT 1 FROM venta_motocicleta x WHERE x.id_motocicleta=m.id_motocicleta);

COMMIT;

SELECT count(*) AS motocicletas_demo FROM motocicleta WHERE chasis_completo LIKE 'IND-DEMO-S2-%';
