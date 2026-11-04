INSERT INTO rol (codigo, nombre, descripcion) VALUES
('ADMIN', 'Administrador', 'Administra cuentas y permisos'),
('ALMACEN', 'Almacén', 'Recepción física'),
('INVENTARIOS', 'Inventarios', 'Registro y consulta de unidades'),
('COMERCIAL', 'Comercial', 'Solicitudes comerciales'),
('FACTURACION', 'Facturación', 'Verificación documental'),
('ENSAMBLAJE', 'Ensamblaje', 'Preparación de unidades')
ON CONFLICT (codigo) DO NOTHING;
