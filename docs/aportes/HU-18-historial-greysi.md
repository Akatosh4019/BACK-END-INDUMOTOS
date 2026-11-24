# HU-18: consulta de historial integral de una motocicleta

Aporte de GreysiLL. Rama `feature/greysi-hu18-historial`, creada desde `origin/main` (`5b37b9b`). Se revisaron las ramas remotas disponibles el 24/11/2026: había consultas de ubicación y seguimiento separadas, pero no una consulta integral por motocicleta. La edición de usuarios queda fuera de este aporte.

## Problema y resultado

Para reconstruir el recorrido de una unidad era necesario consultar distintos módulos. El nuevo endpoint reúne hechos ya guardados y los ordena cronológicamente, con fecha, responsable, origen/destino y solicitud cuando corresponden. No registra hechos nuevos ni altera el inventario.

`GET /api/v1/motocicletas/{id}/historial-integral?desde=2026-11-01&hasta=2026-11-24&pagina=0&tamano=20`

Primero se utiliza la búsqueda existente `/api/v1/motocicletas?chasis=0042`. Si hay coincidencias se elige por modelo/color/chasis y se usa el ID de esa unidad para el historial. No se considera único un sufijo de cuatro caracteres.

Requiere sesión y `INVENTARIO_CONSULTAR`, utilizando la regla existente de `SecurityConfig` para GET de motocicletas. No modifica los permisos de ningún rol. Un usuario sin sesión obtiene 401; sin permiso, 403. El módulo no admite escrituras.

## Contrato

- `motocicleta`: ID y datos actuales de identificación (chasis, marca, modelo, color).
- `items`: clave estable de evento, fecha original con zona, tipo, descripción, usuario registrado, origen, destino e ID de solicitud cuando aplica.
- `pagina` comienza en cero; tamaño predeterminado 20, entre 1 y 100; incluye `totalElementos` y `totalPaginas`. Una página fuera del total devuelve items vacíos.
- `desde` y `hasta` son fechas AAAA-MM-DD de Perú. Ambos días se incluyen completos: se consulta desde medianoche inicial hasta medianoche exclusiva del día siguiente al final. Parámetros inválidos devuelven 400; moto inexistente, 404.
- `fuentesDisponibles` indica los módulos instalados que se consultaron, no afirma que todos contengan eventos para esa moto.

Orden ascendente por fecha original, tipo de fuente y clave estable. El total y la página se consultan bajo una transacción de lectura REPEATABLE_READ para conservar la misma vista de los datos.

## Fuentes y compatibilidad

En el esquema de `main` V1–V23 se incluyen llegada, registro de ficha, ubicación/traslado, asignación y retiro de unidad y avances de solicitud ocurridos mientras la unidad estuvo asociada. La selección por EXISTS evita repetir un evento por varias asociaciones; no atribuye a una moto avances posteriores a retirarla o anteriores a asignarla.

Cuando existen las tablas de las otras ramas oficiales, se agregan:

| Esquema | Hechos |
| --- | --- |
| V24 | Despacho y recepción confirmada por unidad |
| V26–V28 | Venta registrada; soporta id_vendedor original y su cambio a id_registrador |
| V29 | Cambios de disponibilidad; reporte y resolución de diferencias |

Se detectan las tablas existentes en el esquema conectado antes de consultar esos módulos. La rama sigue partiendo de main: no incorpora commits de terceros ni requiere anticipar su integración. No hay nuevas migraciones, tablas, semillas o modificaciones de archivos existentes.

No se devuelven importes, comprobantes, referencias de pago, contraseñas, contactos, fotos ni valores anteriores/nuevos de diferencias. La consulta no recalcula disponibilidad ni confunde ubicación con venta. Una recepción es un hecho de llegada y su fecha es la guardada por el sistema, no una fecha física supuesta.

## Límites explícitos

Este endpoint integra hechos registrados; no inventa eventos faltantes. La cabecera contiene los datos actuales de la moto, no una fotografía histórica. Ediciones antiguas de ficha sin auditoría no pueden reconstruirse. Los movimientos manuales y las recepciones de envío son fuentes distintas y conservan sus claves, aunque una recepción haya generado también un movimiento. Notas de logística y motivos privados no se copian a esta vista.

La validación funcional con el equipo y el DoD del backlog sigue siendo necesaria antes de marcar HU-18 como aceptada. Esta entrega es backend; no añade todavía la pantalla web.

## Pruebas

`ConsultarHistorialMotocicletaTest`: validación, ausencia y límites de día en Perú.
`HistorialMotocicletaAccesoTest`: cadena real de autorización, 401/403, contrato de lectura, fechas inválidas y rechazo de escritura.
`HistorialMotocicletaPostgresTest`: unión SQL real, orden, páginas, aislamiento por moto, ventana de asignación, fuentes opcionales, compatibilidad de columna de ventas y ausencia de información financiera.

Las pruebas PostgreSQL utilizan tablas temporales, search_path aislado y rollback, sin modificar datos del proyecto. Para activarlas: INDUMOTOS_TEST_DB_URL, INDUMOTOS_TEST_DB_USER y INDUMOTOS_TEST_DB_PASSWORD (no versionar sus valores). Sin esas variables se omiten expresamente; se debe registrar si se ejecutaron al revisar el PR.

En el esquema main: 93 pruebas pasaron, cero omitidas, con PostgreSQL habilitado. Comprobación adicional en una copia temporal de origin/feature/roberto-indumotos (9bbad83) con los mismos archivos añadidos: 113 pruebas aprobadas, cero omitidas. No se fusionó esa rama ni se modificaron sus archivos.

## Presentación del aporte

Mostrar búsqueda por chasis, elección de una moto y respuesta de historial con fecha/actor. Demostrar una búsqueda por fechas y una cuenta sin permiso. Explicar que el aporte lee los eventos del sistema existente y no vuelve a implementar usuarios, disponibilidad o ventas.

Para atribución en GitHub: el commit debe usar el correo vinculado a GreysiLL. La rama y el PR evidencian el aporte antes de integrarlo; el panel Contributors solo lo cuenta tras integrarlo a la rama predeterminada, según las reglas de GitHub. La aprobación y fusión corresponden al equipo.
