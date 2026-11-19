# Guía de prueba visual — Base logística DAM

## Qué se actualizó

Se agregó la base de datos para registrar ubicaciones reales, movimientos de motocicletas y solicitudes de ensamblaje. También se incorporó trazabilidad: los movimientos y eventos históricos no se pueden modificar ni eliminar.

## 1. Levantar la base de datos

Desde la carpeta del backend ejecuta:

```powershell
docker compose up -d db
docker compose ps
```

La base debe aparecer con estado `Up` o `healthy`.

## 2. Conectarse desde DBeaver o pgAdmin

- Host: `127.0.0.1`
- Puerto: `5433`
- Base de datos: `indumotos`
- Usuario y contraseña: los valores configurados en el archivo `.env`

## 3. Confirmar las migraciones

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Las versiones `8` y `9` deben aparecer con `success = true`.

## 4. Revisar visualmente los objetos nuevos

Tablas: `ubicacion`, `movimiento_motocicleta`, `solicitud_ensamblaje`, `solicitud_ensamblaje_detalle`, `solicitud_ensamblaje_evento` y `solicitud_ensamblaje_unidad`.

Vistas: `motocicleta_ubicacion_actual` y `solicitud_ensamblaje_estado_actual`.

## Capturas recomendadas

1. Contenedores ejecutándose en Docker Desktop.
2. Historial de Flyway mostrando las versiones 8 y 9 exitosas.
3. Lista de las seis tablas nuevas.
4. Estructura de `movimiento_motocicleta` con sus llaves foráneas.
5. Estructura de `solicitud_ensamblaje_evento` y las dos vistas.

> No ejecutes `docker compose down -v`, porque `-v` elimina el volumen de la base de datos.

