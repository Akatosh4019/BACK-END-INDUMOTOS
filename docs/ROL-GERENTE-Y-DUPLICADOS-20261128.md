# Rol Gerente y registros duplicados — 28/11/2026

Rama `feature/ismael-rol-gerente` (base `feature/ismael-catalogo-ventas`, 67e54f2). Nota para integrar en `main` y en la web.

## Motivo

Observación del profesor: Administración concentraba todas las decisiones del negocio. Gerencia decide; Administración solo gestiona personal y accesos. Además, el sistema permitía registrar dos veces la misma guía de remisión o el mismo número de motor.

## Cambios

| Commit | Cambio |
|---|---|
| 78765ff | V35 crea GERENTE y deja a ADMIN solo USUARIOS_CREAR, USUARIOS_CONSULTAR, USUARIOS_DESACTIVAR y PERMISOS_ASIGNAR. Retira excepciones PERMITIR operativas de cuentas ADMIN. Catálogo y compañía pasan a ser exclusivos de GERENTE; ventas por tienda y diferencias reconocen a GERENTE en lugar de ADMIN. |
| edb72e0 | V36 agrega índices únicos sobre `upper(btrim(numero_guia))` y `upper(btrim(numero_motor))`. Llegadas, fichas, edición y borradores responden 409 con mensaje claro. |

Permisos de GERENTE: INVENTARIO_CONSULTAR, UBICACIONES_CONSULTAR, SOLICITUDES_CONSULTAR, SOLICITUDES_ANULAR, DISPONIBILIDAD_CONSULTAR, DIFERENCIAS_CONSULTAR, TIENDA_CONSULTAR, COMPANIA_CONSULTAR, CATALOGO_GESTIONAR, REPORTES_CONSULTAR, PAGOS_CONFIRMAR.

## Verificación

- `mvn test`: 126 pruebas, 0 fallos ni errores.
- Base de demostración local `indumotos_evolucion_20261124`: respaldo previo `indumotos_evolucion_20261124-antes-V35-20261128.dump` (32 tablas, fuera del repositorio). Imagen `indumotos-evolucion:edb72e0` aplicó V35 y V36; sin guías ni motores repetidos previos.
- Cuenta `gerente.indumotos` creada por el responsable mediante la API de Administración (sin publicar contraseña). En Android ve Inventario, Traslados y Solicitudes.
- Guía `T001-00000001` registrada desde el Samsung; el segundo intento fue rechazado y la base conserva un único registro.

## Para integrar en main (Roberto)

1. `main` usa V32–V34 con contenidos distintos a esta rama; V35 y V36 deben entrar después de conciliar esas versiones en una copia aislada con respaldo. No renumerar migraciones aplicadas.
2. La web debe ofrecer el rol GERENTE al crear cuentas y dejar de mostrar a ADMIN con todos los permisos fijos.
3. Validar en la web la guía como serie-número SUNAT (`T001-00012345`), igual que el móvil, y mostrar el 409 de guía o motor repetido.
4. Crear `gerente.indumotos` en la base del equipo desde Administración, con la contraseña que acuerde el equipo.

No se modificó la base del equipo ni `main`.
