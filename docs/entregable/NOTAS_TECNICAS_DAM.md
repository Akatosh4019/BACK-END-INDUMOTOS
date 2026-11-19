# Notas técnicas para el entregable DAM

## Incremento: base logística y trazabilidad

El backend utiliza PostgreSQL como base de datos y Flyway para crear y actualizar su estructura de forma controlada. Cada cambio queda numerado en archivos de migración, por lo que el equipo puede demostrar qué versión de la base está instalada.

En este incremento se modelaron ubicaciones, movimientos de motocicletas y solicitudes de ensamblaje. Las relaciones se protegen con llaves foráneas; las cantidades, estados y datos obligatorios se validan con restricciones; y los índices facilitan las búsquedas frecuentes.

La ubicación de una motocicleta no se inventa ni se guarda como un dato aislado. Se obtiene de su último movimiento confirmado. Los movimientos y eventos forman un historial inmutable: no se modifican ni se eliminan.

## Evidencias disponibles

- Migraciones `V8` y `V9`.
- Prueba SQL automatizada de reglas de negocio.
- Prueba de migraciones V1 a V9 en PostgreSQL 18.
- Prueba de actualización V7 a V9 conservando exactamente el usuario, la llegada y la motocicleta existentes del Sprint 1.
- Guía de validación visual para Docker, DBeaver o pgAdmin.

## Validación pendiente

La compilación Maven debe repetirse cuando exista acceso a las dependencias remotas. En la sesión de desarrollo, la descarga estuvo bloqueada por la conexión disponible; esto no representa un error detectado en el código.

