# Indumotos: análisis para terminar la web

Análisis del backlog local `Product Backlog - Indumotos - puntuacion y tarjetas completas.xlsx` (hojas «Sprint 3», «Puntuación general» y «DoR y DoD») frente al código y `docs/SISTEMA_IMPLEMENTADO.md` de la rama `sprint-2`. Es un plan, no una declaración de historias terminadas. Android queda fuera del alcance solicitado.

## Estado de partida

- Sprint 1 y Sprint 2 tienen flujos web y API implementados: acceso por rol, llegadas con fotos, OCR asistido, borradores de fichas, inventario, catálogo, solicitudes, asignación, despacho, recepción y ubicación confirmada. El cierre de Sprint 2 aún necesita revisión visual por rol, evidencia y prueba de punta a punta con una guía autorizada. La regla de anulación de HU-27 sigue por acordar.
- El backlog **solo asigna un sprint posterior: Sprint 3** (HU-10 a HU-13, 16 puntos propuestos). Las otras 11 HU son candidatas **sin sprint ni fecha**; los puntos son estimaciones preliminares, no compromiso. No existe un Sprint 4 formal en esta fuente.
- No se encontró en el código un flujo de estado comercial de disponibilidad ni de incidencias por datos para HU-10 a HU-13. `usuario.estado` sí existe, y la lista paginada de cuentas también, pero falta el flujo auditado de desactivar/reactivar.

## Sprint 3: disponibilidad e inconsistencias

| HU | Qué falta implementar | Conexión con el sistema actual | Acordar antes de programar |
| --- | --- | --- | --- |
| HU-10 Consultar disponibilidad | Mostrar estado vigente, motivo/fuente y fecha de cada unidad al buscarla por chasis o abrir su ficha. Avisar si no hay dato confiable. | Añadir al detalle/listado de `motocicleta` una proyección del último evento de disponibilidad. Comercial y facturación solo consultan según permiso; conservar la búsqueda por chasis/filtros, nunca exigir ID al trabajador. | Estados exactos y cuándo una unidad cuenta como disponible. |
| HU-11 Cambiar disponibilidad | Cambio autorizado con estado previo/nuevo, fecha, actor y fuente o motivo; evitar sobrescribir historial. | Nueva tabla de eventos ligada por FK a `motocicleta`; endpoint de transición con validación de estados y permiso propio; web en ficha o módulo de disponibilidad. No inferir «vendida» solo porque salió de almacén o fue recibida en tienda. | Quién confirma pago/venta y qué comprobante o sistema externo aporta la verdad; sin integración automática presupuestada. |
| HU-12 Reportar diferencia | Facturación registra moto, campo discrepante, valor observado, explicación y autor; Inventarios ve el caso pendiente. | Tabla de casos y eventos ligada a `motocicleta`; búsqueda por chasis, alta y bandeja paginada por estado; notificación dentro del sistema a Inventarios. | Campos obligatorios y responsable receptor. |
| HU-13 Resolver diferencia | Investigación, fuente comprobada, valores anterior/corregido, responsable y cierre; aviso al reportante. | Vincular el caso a la edición existente de ficha (`FICHAS_EDITAR`), pero exigir permiso de resolución y transacción que guarde corrección y auditoría juntas. El caso se consulta desde la ficha. | Quién autoriza corregir y valida el cierre. |

**Secuencia técnica sugerida:** definir estados y permisos → migración Flyway V26 o posterior para eventos de disponibilidad y casos de diferencia, índices/FK y restricciones → API de consulta/transición/reporte/resolución → Angular con búsqueda y bandejas por rol → pruebas de permisos, transiciones, concurrencia, auditoría y flujo completo por gateway. No modificar migraciones V1–V25 ya aplicadas. Mantener `motocicleta` como entidad central; `llegada`, ubicación, solicitud y envío siguen representando hechos distintos.

**Regla de negocio clave:** ubicación física, reserva/asignación y disponibilidad comercial no son sinónimos. Una moto puede estar confirmada en una tienda y aun así no estar disponible para venta. La pantalla debe mostrar cada dimensión con su fuente y fecha.

## Historias sin sprint: decisión y trabajo pendiente

| HU | Situación observada y siguiente paso |
| --- | --- |
| HU-21 Recepción con diferencias | `cantidadDeclarada` y `cantidadRecibida` ya distinguen faltantes; acordar si se necesita caso formal por guía/moto, evidencia y resolución antes de marcarla completa. |
| HU-23 Anular recepción | Sin flujo formal de anulación de llegada y reversión de fichas; definir autorizador, efectos y auditoría antes de construirlo. |
| HU-25 Leer guía con ayuda | OCR local sobre todas las fotos propone borradores y revisión humana; medirlo con guías reales, incertidumbre y privacidad antes de aceptar el DoD. |
| HU-03 Filtrar inventario | Filtros combinados y limpieza implementados; verificar por rol, paginación y evidencia real. |
| HU-18 Consultar historial | Hay historial de ubicación y seguimiento de solicitud; definir si el DoD exige un historial unificado de **todos** los eventos de una moto, retención y actores visibles. |
| HU-19 Seguir solicitud | Seguimiento y eventos implementados; verificar que Comercial solo vea sus solicitudes y no edite trabajo logístico. |
| HU-20 Ver trabajo pendiente | Hay listas de guías y solicitudes; falta confirmar si se requiere una bandeja única con tipo, antigüedad, prioridad y enlace al original. |
| HU-22 Ver motivo de no disponibilidad | Depende de HU-10/11: mostrar motivo registrado o «sin motivo registrado», sin inventarlo. |
| HU-24 Reportar incidencia física | No confundir con error de datos (HU-12); definir incidentes por moto/movimiento, evidencias, estado y resolución. |
| HU-16 Desactivar/reactivar cuenta | Existe `usuario.estado` y seguridad relee cuenta activa; faltan acciones autorizadas, revocación de sesiones y bitácora de cambios. |
| HU-17 Consultar usuarios | Lista paginada de 10 y permisos implementados; comprobar acceso exclusivo de Administración y exposición mínima de datos personales. |

Estas historias no se deben marcar `Done` solo por tener una pantalla parecida. Primero comparar cada DoD del Excel con pruebas y evidencia; después asignar las brechas a un sprint nuevo con capacidad y prioridad acordadas.

## Cierre antes de fusionar o declarar terminado

1. Validar con usuarios de Almacén, Inventarios, Comercial y Administración el recorrido llegada → ficha → solicitud → despacho → recepción, incluida una guía real autorizada y sus fotos.
2. Confirmar reglas abiertas: anulación HU-27, disponibilidad/venta, correcciones, recepción con diferencias y responsabilidades. Registrar decisiones en este plan antes de cambiar el esquema.
3. Ejecutar pruebas backend, compilación y pruebas Angular, migraciones en PostgreSQL y prueba por gateway; guardar capturas verdaderas en las carpetas de evidencia de cada tarea. No alterar datos reales para hacer coincidir una demo.
4. Cuando el incremento esté aceptado, integrar `sprint-2` a `main` según el flujo del equipo. Este análisis no fusiona ramas ni implementa Sprint 3.
