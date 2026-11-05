# HU-15-T05 — Asignación y aplicación de permisos

Fecha de verificación: 05/11/2026 (hora de Lima).

## Implementación

- Flyway V5 registra los cinco permisos de Sprint 1 y las asignaciones iniciales de la matriz HU-15-T01: ADMIN crea cuentas, administra accesos y consulta inventario; ALMACEN registra llegadas y consulta inventario; INVENTARIOS registra fichas y consulta inventario.
- `PermisoService` calcula el permiso vigente desde el rol de la cuenta y su excepción individual. `DENEGAR` prevalece; `PERMITIR` habilita una operación operativa ausente del rol. Crear cuentas y administrar accesos quedan reservados al rol ADMIN aun si se intenta conceder una excepción a otro rol.
- El backend consulta la base en cada solicitud autorizada, por lo que un cambio de rol o excepción afecta también a los JWT ya emitidos. Una cuenta o rol inactivo no obtiene permisos.
- `PermisoController` expone `GET /api/v1/usuarios/{id}/permisos`, `PUT /api/v1/usuarios/{id}/rol`, `PUT /api/v1/usuarios/{id}/permisos/{codigo}` y `DELETE /api/v1/usuarios/{id}/permisos/{codigo}`. Estas rutas exigen `PERMISOS_ASIGNAR`.
- Las rutas existentes de creación de cuentas, llegadas, fichas y consulta usan permisos por acción. Las rutas no implementadas se deniegan por defecto.

## Pruebas realizadas

`mvn -o -q clean verify`: 14 pruebas, 0 fallos y 0 errores. Tres pruebas nuevas verifican permiso del rol, prioridad de `DENEGAR`, concesión `PERMITIR`, cambio de rol, retirada de excepciones, cuenta inactiva y reserva de permisos administrativos.

Con Docker local, Flyway V5 quedó `success=true` y hay cinco permisos de Sprint 1. La prueba HTTP reproducible `docs/evidencias/HU-15-T05/verificar-permisos.ps1` pasó: login 200; ADMIN sin permiso operativo 403; creación de cuentas 201; consulta sin token 401 y sin permiso 403; consulta autorizada 200; `DENEGAR` sobre `FICHAS_REGISTRAR` cambió el registro a 403; `PERMITIR` sobre `LLEGADAS_REGISTRAR` permitió registrar una llegada 201; cambio de rol surtió efecto con el mismo token; retirar las excepciones restauró los permisos del rol; intento de conceder `USUARIOS_CREAR` a INVENTARIOS devolvió 403. Las cuentas y la llegada ficticias de esta prueba se retiraron; consulta posterior confirmó cero cuentas `hu15_%`.

## Capturas para el expediente

Importar `docs/postman/Indumotos-Sprint-1.postman_collection.json` y ejecutar en orden sobre el entorno local. Configurar `adminPassword` como variable secreta y evitar que el token o la clave aparezcan en las capturas. Guardar en la carpeta de HU-15/T05 al menos:

1. Consulta inicial de permisos INVENTARIOS: `FICHAS_REGISTRAR` permitido por rol y `LLEGADAS_REGISTRAR` no permitido.
2. `DENEGAR` para `FICHAS_REGISTRAR`: respuesta de consulta con `permitido=false` y solicitud de registro posterior con HTTP 403.
3. `PERMITIR` para `LLEGADAS_REGISTRAR`: `permitido=true` y registro de llegada HTTP 201.
4. Cambio de rol a ALMACEN: `rolCodigo=ALMACEN` y registro de ficha HTTP 403 usando el mismo token.
5. Consulta de permisos sin token HTTP 401 y consulta con ALMACEN HTTP 403.
6. Retirada de excepciones y regreso a INVENTARIOS: permisos efectivos restaurados.

Usar solo datos ficticios. La matriz HU-15-T01 es una definición inicial del equipo; sus cruces concretos pueden ajustarse cuando Indumotos los valide. El cierre de HU-15 completa requiere también las tareas de interfaz y prueba de la historia.
