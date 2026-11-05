-- Los registros repetidos de guías DEMO anteriores a esta regla se conservan fuera del inventario activo.
CREATE TABLE motocicleta_duplicado_archivo AS
SELECT m.*, CURRENT_TIMESTAMP::timestamptz AS archivado_en
FROM motocicleta m WHERE false;

WITH repetidas AS (
    SELECT m.id_motocicleta,
           row_number() OVER (PARTITION BY upper(btrim(m.chasis_completo)) ORDER BY m.id_motocicleta) AS numero
    FROM motocicleta m
), demos AS (
    SELECT m.* FROM motocicleta m
    JOIN repetidas r ON r.id_motocicleta = m.id_motocicleta
    JOIN llegada l ON l.id_llegada = m.id_llegada
    WHERE r.numero > 1 AND l.numero_guia LIKE 'GR-DEMO-HU02-%'
)
INSERT INTO motocicleta_duplicado_archivo
SELECT demos.*, CURRENT_TIMESTAMP FROM demos;

DELETE FROM motocicleta m USING motocicleta_duplicado_archivo a
WHERE m.id_motocicleta = a.id_motocicleta;

-- Si existen otros duplicados, la migración falla para revisarlos manualmente y no borrar datos reales.
CREATE UNIQUE INDEX ux_motocicleta_chasis_completo
ON motocicleta (upper(btrim(chasis_completo)));
