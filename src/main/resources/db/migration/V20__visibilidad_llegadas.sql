-- Permite retirar guías de ensayo del listado sin borrar su historial logístico.
ALTER TABLE llegada ADD COLUMN visible_en_listado BOOLEAN NOT NULL DEFAULT true;
