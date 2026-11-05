# HU-03-T03 — Consulta filtrada de fichas

Fecha de ejecución: 05/11/2026.  
Entorno: API Spring Boot y PostgreSQL locales con Docker; datos de prueba ficticios.

## Resultado

La implementación pasó `mvn -q clean verify` y la comprobación HTTP real descrita en `resultado-http.json`.

| Caso | Resultado |
| --- | --- |
| Consulta por `marca=RONCO` y `color=ROJO` | `200` y una ficha resumida. |
| Sin coincidencias | `200`, lista vacía y total en cero. |
| Página negativa | `400` con mensaje de validación. |
| Sin token JWT | `401`. |
| Usuario con rol COMERCIAL, sin `INVENTARIO_CONSULTAR` | `403`. |

La respuesta de listado solo expone `id`, `marca`, `modelo`, `color` y `chasisVista`. No expone número de motor, DUA, ítem DUA, usuario creador ni el chasis completo.

## Capturas de Postman

- `02-filtro-ronco-rojo-200.png`
- `03-sin-coincidencias-200.png`
- `04-pagina-invalida-400.png`
- `05-sin-autenticacion-401.png`
- `07-rol-sin-permiso-403.png`

Las capturas de inicio de sesión no se almacenan porque muestran un token JWT completo. El resultado de autenticación se registra en los casos de prueba sin exponer secretos.

## Diseño implementado

Los filtros de marca, modelo y color son opcionales y se combinan con **AND**. Si no se envía ninguno, se obtiene el inventario paginado. El tamaño admitido está entre 1 y 100 y la página comienza en cero.
