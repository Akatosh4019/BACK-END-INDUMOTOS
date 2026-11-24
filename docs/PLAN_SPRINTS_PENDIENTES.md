# Indumotos: alcance y verificación de Sprint 3

Análisis del backlog local `Product Backlog - Indumotos - puntuacion y tarjetas completas.xlsx` (hojas «Sprint 3», «Puntuación general» y «DoR y DoD») frente al código y `docs/SISTEMA_IMPLEMENTADO.md` de la rama `sprint-2`. Es un plan, no una declaración de historias terminadas. Android queda fuera del alcance solicitado.

## Estado de partida

- Sprint 1 y Sprint 2 tienen flujos web y API implementados: acceso por rol, llegadas con fotos, OCR asistido, borradores de fichas, inventario, catálogo, solicitudes, asignación, despacho, recepción y ubicación confirmada. El cierre de Sprint 2 aún necesita revisión visual por rol, evidencia y prueba de punta a punta con una guía autorizada. La regla de anulación de HU-27 sigue por acordar.
- El backlog **solo asigna un sprint posterior: Sprint 3** (HU-10 a HU-13, 16 puntos propuestos). Las otras 11 HU son candidatas **sin sprint ni fecha**; los puntos son estimaciones preliminares, no compromiso. No existe un Sprint 4 formal en esta fuente.
- Después de este análisis se implementaron disponibilidad y diferencias (V29, API y web). El flujo auditado de desactivar/reactivar cuentas llegó después con V30; faltan pruebas visuales por rol.

## Sprint 3: disponibilidad e inconsistencias

| HU | Código implementado | Conexión con el sistema actual | Verificación o decisión pendiente |
| --- | --- | --- | --- |
| HU-10 Consultar disponibilidad | Búsqueda por chasis y estado derivado de venta, tránsito, bloqueo, asignación y ubicación. Muestra motivo, fecha y sede. | Panel «Disponibilidad y datos» ligado a `motocicleta`, venta, envío y ubicación. | Validar con trabajadores los nombres de estados y casos sin dato confiable. |
| HU-11 Cambiar disponibilidad | Facturación/Admin registra eventos `BLOQUEADA` o `HABILITADA` con motivo, autor y fecha; conserva historial. | V29 y API de disponibilidad; una venta se registra por el flujo de Facturación con pago y comprobante verificados. | Probar transiciones con casos reales; acordar estados comerciales adicionales. |
| HU-12 Reportar diferencia | Facturación/Admin abre caso por moto y campo; Inventarios ve la bandeja de pendientes. | V29, API y panel con búsqueda por chasis; cada caso conserva autor y fecha. | Probar reporte real y confirmar si se necesita aviso externo. |
| HU-13 Resolver diferencia | Inventarios/Admin contrasta fuente, corrige ficha y cierra caso en una transacción con valor anterior, nuevo, actor y fecha. | Reutiliza `EditarMotocicleta` y `FICHAS_EDITAR`; reportante ve el caso resuelto en su bandeja. | Validar corrección y cierre con una discrepancia autorizada. |

**Implementación:** Flyway V29 agrega eventos y casos; la API valida permisos y transiciones; Angular usa búsqueda y bandeja paginada por rol. Las pruebas automáticas y consultas de solo lectura por gateway pasaron; faltan pruebas visuales y un caso real autorizado. No se modificaron migraciones ya aplicadas. `motocicleta` sigue siendo la entidad central; llegada, ubicación, solicitud y envío son hechos distintos.

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
| HU-22 Ver motivo de no disponibilidad | Adelantada del backlog sin sprint e implementada en `feature/gutierrez-hu16-hu17-hu22`: cada consulta devuelve un solo estado (VENDIDA, EN_TRANSITO, BLOQUEADA, EN_REVISION, ASIGNADA, SIN_UBICACION o DISPONIBLE), su motivo, fecha, quién lo registró y `puedeOfrecerse`. Una diferencia de datos pendiente deja la moto EN_REVISION y bloquea su venta. Falta validación visual con usuarios. |
| HU-24 Reportar incidencia física | No confundir con error de datos (HU-12); definir incidentes por moto/movimiento, evidencias, estado y resolución. |
| HU-16 Desactivar/reactivar cuenta | Adelantada del backlog sin sprint e implementada en `feature/gutierrez-hu16-hu17-hu22` (V30): `PUT /api/v1/usuarios/{id}/estado` con motivo obligatorio, bitácora `usuario_estado_evento`, cierre de sesiones abiertas, sin desactivar la propia cuenta ni al último administrador activo. Falta validación visual con usuarios. |
| HU-17 Consultar usuarios | Adelantada del backlog sin sprint e implementada en `feature/gutierrez-hu16-hu17-hu22`: `GET /api/v1/usuarios/{id}` muestra estado, rol, tienda, permisos efectivos, excepciones y último cambio; permiso `USUARIOS_CONSULTAR` exclusivo de Administración. No expone contraseña ni datos personales adicionales. |

Estas historias no se deben marcar `Done` solo por tener una pantalla parecida. Primero comparar cada DoD del Excel con pruebas y evidencia; después asignar las brechas a un sprint nuevo con capacidad y prioridad acordadas.

## Cierre antes de fusionar o declarar terminado

1. Validar con usuarios de Almacén, Inventarios, Comercial y Administración el recorrido llegada → ficha → solicitud → despacho → recepción, incluida una guía real autorizada y sus fotos.
2. Confirmar reglas abiertas: anulación HU-27, disponibilidad/venta, correcciones, recepción con diferencias y responsabilidades. Registrar decisiones en este plan antes de cambiar el esquema.
3. Ejecutar pruebas backend, compilación y pruebas Angular, migraciones en PostgreSQL y prueba por gateway; guardar capturas verdaderas en las carpetas de evidencia de cada tarea. No alterar datos reales para hacer coincidir una demo.
4. Cuando el incremento esté aceptado, integrar `sprint-2` a `main` según el flujo del equipo. Este análisis no fusiona ramas ni implementa Sprint 3.

## Ampliación: dos modos para trabajadores y ventas internas

El enfoque acordado es para **trabajadores**. `Modo compañía` permite a Administración ver tiendas, stock y ventas y asignar una tienda a cuentas de Comercial o Facturación. `Modo tienda` limita a cada trabajador al stock y ventas de su sede; Administración puede abrir una tienda concreta. V26 guarda la asignación y las ventas por motocicleta, comprobante, importe, usuario y fecha; V27 agrega referencia del pago y mueve el permiso de registro a Facturación. V28 nombra al usuario como registrador, sin asumir que atendió al cliente.

Según el resumen consolidado de la entrevista 4, Comercial origina la reposición y Facturación actualiza el estado del stock. La transcripción automática de la entrevista 5, aún sin validación del entrevistado, indica que la moto deja de estar disponible cuando el cliente paga y se emite boleta o factura. Por ello Facturación registra la venta solo después de verificar ambos hechos y aporta sus referencias. El sistema **no cobra** ni emite comprobantes. La API comprueba que la moto esté físicamente confirmada en esa tienda, sin tránsito ni venta anterior, y la bloquea durante la operación para evitar duplicados. Una moto vendida desaparece del stock y no puede asignarse o trasladarse como inventario disponible.

La integración futura con Yape/banco exige definir proveedor, credenciales, conciliación y confirmación segura del pago; no se representa un cobro externo como si ya existiera. El catálogo público y checkout quedan fuera de esta etapa laboral. HU-10/11 todavía requieren decidir estados comerciales adicionales, como reserva, exhibición o indisponibilidad temporal.
