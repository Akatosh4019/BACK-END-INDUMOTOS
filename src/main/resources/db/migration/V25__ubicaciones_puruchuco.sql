-- Puruchuco tiene almacén y tienda en la misma sede física; se distinguen
-- porque una unidad puede seguir bajo custodia de almacén o ya estar en tienda.
INSERT INTO ubicacion(codigo,nombre,tipo) VALUES
('PURUCHUCO_ALM','Almacén Puruchuco','ALMACEN'),
('PURUCHUCO_TIENDA','Tienda Puruchuco','TIENDA')
ON CONFLICT DO NOTHING;
