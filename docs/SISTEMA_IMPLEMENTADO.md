# Indumotos: sistema implementado en `sprint-2`

Esta guía describe el código actual del backend y su relación con la web. Cubre los incrementos desarrollados de Sprint 1 a Sprint 3 en la rama `sprint-2`. La aceptación visual y las historias sin sprint siguen pendientes; esta guía no sustituye las bitácoras ni las evidencias.

## Arquitectura y datos

- Java 21 y Spring Boot. `presentation` expone la API HTTP, `application` contiene los casos de uso, `domain` los registros del negocio e `infrastructure` la persistencia JDBC y la seguridad.
- PostgreSQL almacena cuentas, permisos, llegadas, fotos, borradores, motocicletas, catálogo, solicitudes, asignaciones, ubicaciones, envíos, ventas, disponibilidad y casos de diferencias. Flyway aplica migraciones V1 a V30; no se editan migraciones ya aplicadas.
- En Docker Compose, `api` se conecta a `db` por la red interna y PostgreSQL usa el volumen `indumotos_pgdata`. Reiniciar o reconstruir contenedores no borra el volumen. `docker compose down -v` sí lo elimina.
- La web Angular usa JWT Bearer. En Docker: navegador → Nginx (`127.0.0.1:4200`) → gateway (`gateway:8030`) → API (`api:8080`) → PostgreSQL. La API comprueba los permisos; el gateway no reemplaza esa comprobación.

## Trabajo por rol

| Rol | Flujo principal |
| --- | --- |
| Administración | Crea cuentas, asigna roles y excepciones de permisos, gestiona catálogo y tiendas, consulta todas las sedes y puede registrar ventas y resolver casos según permisos. `CATALOGO_GESTIONAR` está reservado a `ADMIN` desde V23. |
| Almacén | Registra una llegada con los datos y fotos de la guía; atiende solicitudes y prepara unidades según sus permisos. |
| Inventarios | Consulta el inventario, registra fichas vinculadas a una guía pendiente, revisa borradores, finaliza el registro y resuelve diferencias de datos con fuente y corrección. |
| Comercial | Solicita combinaciones del catálogo, cantidades y sede de destino; consulta seguimiento, stock y disponibilidad de su tienda. No modifica catálogo ni confirma ventas. |
| Facturación | Consulta su tienda; tras verificar pago y boleta/factura registra la venta y reporta diferencias de datos. |
| Ensamblaje | Participa en operaciones de seguimiento según los permisos asignados. |

La matriz efectiva se calcula desde `rol_permiso` y `usuario_permiso`; `DENEGAR` prevalece. Un permiso administrativo sensible no puede concederse por excepción a un rol no administrador. La API relee el estado de la cuenta y sus permisos para cada solicitud protegida.

## Flujo de guía a inventario

1. Almacén envía `POST /api/v1/llegadas/con-guia` con `multipart/form-data`: número de guía, cantidades declarada y recibida, observación opcional y entre 1 y 3 fotos. La llegada nace `PENDIENTE` por defecto (V22). El registro anterior de llegadas sigue disponible sin cambiar sus campos esenciales.
2. El backend limita cada foto a 5 MB, JPG o PNG, dimensiones mínimas de 600 px por lado y máximo 20 megapíxeles; valida el archivo en el servidor. Las fotos se guardan vinculadas a la llegada en PostgreSQL (V19) y se sirven solo a cuentas autorizadas con `Cache-Control: no-store`.
3. Inventarios consulta `GET /api/v1/llegadas/pagina?estado=PENDIENTE&pagina=0`. El filtro por número de guía es opcional. La paginación de llegadas usa 10 registros. `GET /api/v1/llegadas/{id}/detalle` devuelve fichas vinculadas y diferencia entre recibidas y fichas.
4. `GET /api/v1/llegadas/{id}/sugerencias-ocr` ejecuta Tesseract local sobre **todas** las fotos. Solo propone campos; puede omitir o leer mal caracteres. No registra motocicletas. Una persona debe comparar chasis, motor, DUA, ítem, marca, modelo y color con la guía.
5. Cada bloque se conserva mediante `PUT /api/v1/llegadas/{id}/registro/borradores/{posicion}` en `borrador_motocicleta` (V21). `GET /api/v1/llegadas/{id}/registro` permite reanudar desde otra sesión o equipo. Guardar un borrador todavía no crea una fila en `motocicleta`.
6. `POST /api/v1/llegadas/{id}/registro/finalizar` recibe `almacenId` y comprueba que el número de borradores más las fichas existentes iguale la cantidad recibida. En una transacción valida e incorpora las motos, registra su primera ubicación confirmada en ese almacén, elimina los borradores, conserva quién y cuándo terminó en `registro_guia` y cambia `llegada.estado_registro` a `CONFIRMADA`. Ante un error, el lote se revierte. La guía pasa a la lista de confirmadas y ya no admite registros nuevos.

Los identificadores `id` de las rutas relacionan registros internamente. El trabajador busca por **número de guía** o **chasis/filtros**, no necesita buscar por ID. El catálogo (`catalogo_motocicleta`, V18) ofrece las combinaciones válidas y solo Administración puede agregar nuevas.

## Otros flujos implementados

- **Sprint 1:** cuentas y roles; login/logout con JWT de 900 segundos y revocación persistente; registro y consulta de llegadas/fichas; chasis único; inventario con filtros combinados y paginación; detalle y edición de fichas con `FICHAS_EDITAR` (V16).
- **Sprint 2:** ubicaciones y movimientos con historial; solicitudes comerciales por combinaciones y destino; seguimiento por eventos (`PENDIENTE`, `EN_PREPARACION`, `ATENDIDA`, `RECHAZADA`, `ANULADA`); asignación exclusiva y retiro de unidades; traslado entre sedes. La anulación implementada exige motivo y se permite solo en estado `PENDIENTE`; la regla final de HU-27 requiere acuerdo del Product Owner.
- Las solicitudes usan modelos/colores del catálogo. Registrar una solicitud no equivale a incorporar motocicletas; Almacén e Inventarios atienden el trabajo posterior según rol y permisos.

## Rutas principales y seguridad

| Ruta | Uso | Permiso relevante |
| --- | --- | --- |
| `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me` | Sesión y perfil | Login público; el resto con sesión |
| `GET /api/v1/llegadas/pagina`, `GET /api/v1/llegadas/{id}/guias/{imagenId}` | Listar guías y ver foto | `INVENTARIO_CONSULTAR` |
| `POST /api/v1/llegadas/con-guia` | Registrar llegada y fotos | `LLEGADAS_REGISTRAR` |
| `PUT /api/v1/llegadas/{id}/registro/borradores/{posicion}`, `POST /api/v1/llegadas/{id}/registro/finalizar` | Borradores y cierre | `FICHAS_REGISTRAR` e `INVENTARIO_CONSULTAR` |
| `GET /api/v1/motocicletas` | Buscar por chasis completo/últimos cuatro dígitos, marca, modelo, color | `INVENTARIO_CONSULTAR` |
| `POST /api/v1/catalogo-motocicletas` | Agregar combinación | `CATALOGO_GESTIONAR`, exclusivo de `ADMIN` |
| `GET/POST /api/v1/solicitudes`, rutas de seguimiento y asignación | Solicitudes y preparación | Permisos específicos de cada acción |
| `GET /api/v1/solicitudes/{id}/envios` y `GET /api/v1/motocicletas/{id}/envio-activo` | Consultar salida y tránsito | `SOLICITUDES_CONSULTAR` y `UBICACIONES_CONSULTAR` respectivamente |
| `POST /api/v1/solicitudes/{id}/envios/despacho` | Almacén registra la salida de las unidades asignadas | `TRASLADOS_REGISTRAR` |
| `POST /api/v1/solicitudes/{id}/envios/recepcion` con `motocicletaId` | Solicitante o admin confirma una unidad físicamente recibida | `RECEPCIONES_CONFIRMAR` |

La lista completa de rutas y reglas está en `src/main/java/pe/indumotos/security/SecurityConfig.java` y los controladores de `presentation`. El backend devuelve 401 sin sesión válida y 403 cuando falta permiso.

La preparación `ATENDIDA` no mueve físicamente las motos. V24 guarda un envío por moto asociada a la solicitud. Almacén puede registrar la salida solo si todas están asignadas y su última ubicación confirmada es un almacén; durante el tránsito esa ubicación no se sustituye. La recepción de cada moto requiere a quien hizo la solicitud o a Administración y crea un movimiento `TRASLADO` con origen, tienda de destino, usuario y fecha. Un traslado manual se bloquea mientras exista un envío abierto. No se inventaron ubicaciones para fichas históricas: al finalizar una guía se elige explícitamente el almacén receptor.

V25 precarga las ubicaciones `PURUCHUCO_ALM` y `PURUCHUCO_TIENDA`, que representan almacén y tienda en Puruchuco. Se distinguen como áreas de custodia aunque compartan local. Administración puede añadir otras ubicaciones reales desde la web.

## Arranque y verificación

1. Copiar `.env.example` a `.env` y configurar valores propios. `.env` y secretos no se versionan.
2. Desde la raíz de este repositorio: `docker compose up -d --build api`. Esto inicia también `db`, aplica Flyway y conserva el volumen.
3. Comprobar `http://127.0.0.1:8080/api/health` y revisar `docker compose logs --tail=100 api` si falla.
4. Para pruebas con Maven y Java 21: `mvn test`. Si Maven no está instalado localmente, usar el contenedor Maven con el repositorio montado.

En la última ejecución observada de `sprint-2` pasaron **101 pruebas backend** y Flyway llegó a **V30** en PostgreSQL local. Eso no reemplaza la revisión visual ni una prueba de cierre real de guía con datos aprobados. Los resúmenes fechados en `docs/SPRINT 2/` documentan el estado de sus entregas originales y pueden contener verificaciones pendientes que después se resolvieron.

## Trabajo aún abierto

### Modos laborales y ventas agregados después de Sprint 2

Administración usa `GET /api/v1/modos/compania` para ver stock y ventas por tienda y `PUT /api/v1/usuarios/{id}/tienda` para asignar una sede a Comercial o Facturación. Esas cuentas solo consultan `GET /api/v1/modos/tienda`, `/stock` y `/ventas` de su tienda asignada; Administración elige `tiendaId`. Las listas de unidades y ventas son de 10 por página. `POST /api/v1/modos/tienda/ventas` exige `VENTAS_REGISTRAR`, que corresponde a Facturación y Administración, y recibe una moto concreta, referencia de pago confirmado, referencia de boleta/factura e importe. La API bloquea la ficha en la transacción y rechaza otra sede, tránsito, moto vendida o comprobante repetido. La venta deja la moto fuera del stock y bloquea nuevos traslados o asignaciones. Flyway V26–V28 conserva las migraciones anteriores y distingue quién registró la venta de quien trató al cliente.

Este módulo **no cobra ni emite comprobantes**: el trabajador debe verificar esos hechos en el proceso externo. La entrevista 4 señala que Comercial inicia reposición y Facturación actualiza stock; la regla de pago más boleta/factura procede de una transcripción automática de la entrevista 5 todavía sin validación del entrevistado.

Sprint 3 agrega V29: `disponibilidad_evento` y `diferencia_datos`. `GET /api/v1/disponibilidad?chasis=...` calcula estado según venta, tránsito, bloqueo, asignación y ubicación; `POST /api/v1/motocicletas/{id}/disponibilidad` registra cambios con motivo e historial. Facturación reporta en `POST /api/v1/diferencias`; Inventarios revisa `GET /api/v1/diferencias` y resuelve mediante `POST /api/v1/diferencias/{id}/resolucion`. La corrección de ficha y el cierre se guardan en una transacción. Comercial solo consulta disponibilidad. La validación visual y una discrepancia real autorizada siguen pendientes.

#### HU-16, HU-17 y HU-22 adelantadas del backlog (aporte de Luciana Gutiérrez)

Estas historias no pertenecen al Sprint 3 oficial (HU-10 a HU-13); estaban en el backlog sin sprint asignado. El aporte se incorporó a `sprint-2` y HU-22 usa V29.


- **HU-17 Consultar cuenta.** `GET /api/v1/usuarios/{id}` devuelve estado, rol, tienda, fecha de creación, permisos efectivos, excepciones PERMITIR/DENEGAR y el último cambio de estado. Requiere `USUARIOS_CONSULTAR`, que solo puede tener Administración.
- **HU-16 Desactivar o reactivar.** `PUT /api/v1/usuarios/{id}/estado` con `{estado, motivo}` requiere `USUARIOS_DESACTIVAR`. La cuenta no se borra. V30 guarda cada cambio en `usuario_estado_evento` (estado anterior y nuevo, motivo, quién y cuándo), consultable en `GET /api/v1/usuarios/{id}/estado/historial`. El cambio fija `usuario.sesiones_validas_desde`, por lo que los tokens emitidos antes responden 401 incluso tras reactivar. Se rechaza con 409 desactivar la propia cuenta, dejar sin administradores activos o repetir el estado actual.
- **HU-22 Motivo de no disponibilidad.** La disponibilidad se decide con una función pura probada (`DisponibilidadService.derivar`) con prioridad VENDIDA, EN_TRANSITO, BLOQUEADA, EN_REVISION, ASIGNADA, SIN_UBICACION, DISPONIBLE. Cada unidad informa motivo, fecha, `registradoPor` y `puedeOfrecerse`. Una diferencia de datos pendiente pone la moto EN_REVISION, la retira del stock de tienda e impide venderla o habilitarla hasta resolver el caso.
- **Corrección de seguridad.** Un token de una cuenta inactiva o con sesión cerrada respondía 500; ahora responde 401 para que la web cierre la sesión.

#### Mejoras de la revisión del 23/11 (aporte de Luciana Gutiérrez)

Atienden las observaciones de la revisión con el equipo. Flyway **V32** agrega columnas y tablas nuevas sin borrar datos.

- **Sesión de una hora.** El token dura 3600 segundos (antes 900). Se puede cambiar con `INDUMOTOS_SESION_SEGUNDOS` (entre 300 y 43200).
- **Catálogo activo/inactivo.** `catalogo_motocicleta.activo`; `PUT /api/v1/catalogo-motocicletas/{id}/estado` con `{activo}` (requiere `CATALOGO_GESTIONAR`). Una combinación inactiva deja de ofrecerse y de aceptarse en nuevas solicitudes; las motos ya registradas y el registro de guías no cambian.
- **Ubicaciones.** `GET /api/v1/ubicaciones?todas=true` incluye las inactivas; `PUT /api/v1/ubicaciones/{id}` edita nombre, tipo y referencia; `PUT /api/v1/ubicaciones/{id}/estado` activa o desactiva (ambas con `PERMISOS_ASIGNAR`). El código no cambia. No se desactiva una sede con motos, envíos, solicitudes abiertas o cuentas activas, y su tipo no cambia si ya tiene historial.
- **Solicitudes.** `GET /api/v1/solicitudes` acepta `buscar` (número, tienda, solicitante, marca, modelo o color) y `estado`, y devuelve el total en la cabecera `X-Total-Count`; la respuesta sigue siendo una lista. `PUT /api/v1/solicitudes/{id}` edita una solicitud **pendiente** de quien la registró o de Administración; lo anterior se guarda en `solicitud_ensamblaje_edicion` (inmutable) y se consulta en `GET /api/v1/solicitudes/{id}/ediciones`. "Eliminar" sigue siendo **anular con motivo** para no perder el historial.
- **Recorrido de una moto.** `GET /api/v1/motocicletas/{id}/recorrido` (requiere `UBICACIONES_CONSULTAR`) une llegada, registro de ficha, ubicaciones, asignaciones, salida (en camino), recepción, bloqueos y venta con origen, destino, responsable y fecha. Solo lee tablas existentes.
- **Estado en el inventario.** `GET /api/v1/disponibilidad/estados?ids=1,2` devuelve el estado de hasta 50 motos (requiere `DISPONIBILIDAD_CONSULTAR`).
- **Reportes.** `GET /api/v1/reportes/ventas?periodo=SEMANA|MES|ANIO` con el permiso nuevo `REPORTES_CONSULTAR` (Administración): unidades e importe, modelos más y menos vendidos, ventas por tienda, serie por día o mes (hora de Lima) y stock por modelo (almacén, tienda, en camino).
- **Mensajes de error.** Catálogo, ubicaciones y solicitudes devuelven `{"error": "motivo"}` en lugar de textos genéricos como "Conflict".
- **Datos demo.** El perfil `demo` agrega ocho ventas ficticias (`GR-DEMO-VENTAS`, motos `IND-DEMO-V-*`) en la tienda de prueba para que Reportes muestre datos. No usan las 20 motos de prueba anteriores.


- Probar visualmente cada rol y guardar capturas reales en las carpetas de evidencia correspondientes.
- Medir OCR con varias guías reales: Tesseract puede confundir caracteres de chasis y DUA. Mantener confirmación humana.
- Acordar con Product Owner el límite de anulación de HU-27 y cerrar tareas de modelado/documentación académica que correspondan.
- Completar las historias fuera del alcance implementado; esta guía no declara finalizado el producto ni Android.
