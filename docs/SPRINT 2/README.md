# Sprint 2 · Indumotos

> Este expediente recoge el estado de las tareas originales cuando se escribió. Para los ajustes posteriores de catálogo, OCR, borradores, estados de guías, pruebas y Docker, consulta [la guía vigente](../SISTEMA_IMPLEMENTADO.md).

Trabajo realizado sobre la rama `sprint-2` de los repositorios existentes de backend y web. No se han modificado Android ni modelos manuales.

| Historia | Entrega verificable | Estado |
| --- | --- | --- |
| HU-05 | Catálogo de sedes, registro de ubicación inicial, consulta de ubicación e historial; pantalla web | Código y pruebas disponibles |
| HU-06 | Solicitudes con líneas, destino y estado inicial; pantalla de creación y consulta | Código y pruebas disponibles |
| HU-07 | Historial de eventos, transiciones controladas y liberación de unidades cuando se rechaza; pantalla de seguimiento | Código y pruebas disponibles |
| HU-08 | Búsqueda, asignación exclusiva y retiro de unidades; controles web | Código y pruebas disponibles |
| HU-09 | Traslado entre sedes con origen, destino y usuario; pantalla compartida de ubicaciones | Código y pruebas disponibles |
| HU-27 | Anulación con motivo y evento conservado | Código disponible; pendiente confirmar con Product Owner el estado límite para anular |

## Evidencia de ejecución

- Backend: `mvn test` con Java 21; **63 pruebas, 0 fallos, BUILD SUCCESS**. Las pruebas cubren reglas de ubicación, solicitudes, seguimiento, asignación y anulación. No sustituyen una ejecución HTTP integral.
- Frontend Angular: `ng build` concluye correctamente; `ng test --watch=false`: **6 pruebas aprobadas**.
- Base de datos: migraciones V8 a V15 aplicadas progresivamente sobre PostgreSQL 18 aislado en `localhost:55440`, base `s2_hu06_verify_live`; V14 y V15 aplicadas con `psql -v ON_ERROR_STOP=1`. Las consultas confirmaron la creación de los permisos `TRASLADOS_REGISTRAR` y `SOLICITUDES_ANULAR`. Esta base es de verificación y no contiene registros reales del proyecto.
- Arranque real de la API usando el classpath de pruebas de Maven: Flyway validó **15 migraciones** en la base aislada `s2_api_integration` y Tomcat inició en `8080`. `GET /api/health` devolvió **200**; `GET /api/v1/solicitudes` sin token devolvió **401**. Angular sirvió la web en `4200` y su proxy devolvió los mismos resultados desde `/api/health` y `/api/v1/solicitudes`. La cuenta administrativa usada para el arranque de prueba recibió una clave aleatoria de ejecución, no compartida.
- Integración SQL reproducible `src/test/resources/sql/verificar_flujo_sprint2.sql`: moto de Sprint 1, dos movimientos, solicitud, estado y asignación se comprobaron mediante consultas reales en PostgreSQL. Ejecutar con `psql -v ON_ERROR_STOP=1 -f ...`; el archivo hace `ROLLBACK` y la consulta posterior confirmó **0 usuarios de prueba persistidos**.
- Integración HTTP reproducible `src/test/scripts/verificar_sprint2_http.ps1`: **PASS** con base PostgreSQL aislada, migraciones Flyway, login JWT, 401/403, llegada y ficha de Sprint 1, ubicación, traslado, solicitud, asignación exclusiva (duplicado 409), rechazo que libera la unidad y anulación con motivo persistida (estado inválido y repetición 409). Los usuarios y claves del ensayo se generan al ejecutar el script; la base `s2_http_verify` conserva solo datos de prueba.
- Intento de `mvn package` offline: las 62 pruebas se ejecutan, pero el empaquetado termina con **BUILD FAILURE** por dependencias de plugins ausentes en el caché Maven, entre ellas `org.tukaani:xz` y `zstd-jni`.
- Intento de `mvn test` offline en gateway: **bloqueado** por `spring-cloud-dependencies:2025.1.3` ausente en el caché Maven. No hay prueba HTTP a través del gateway.

## Pendiente para cerrar el Sprint

1. Descargar las dependencias Maven del empaquetado y gateway en un equipo con acceso a Maven Central; ejecutar `mvn package` en backend y `mvn test` en `gatewayIndumotos`.
2. Levantar PostgreSQL con Docker Desktop en el equipo del usuario y realizar la revisión visual de las pantallas reales. El recorrido HTTP automático del backend, la regresión básica de Sprint 1 y el proxy de Angular ya fueron comprobados con PostgreSQL aislado; el gateway queda pendiente.
3. Solicitar al Product Owner la regla definitiva de HU-27. La implementación actual permite anular solamente solicitudes `PENDIENTE` y exige motivo.
4. Tomar capturas reales desde la interfaz cuando Roberto haga las pruebas visuales. No se han fabricado capturas.
5. Revisar y subir ambos repositorios remotos cuando se habilite el acceso al Git remoto.

Los commits registran las fechas reales de ejecución. No se asignan horas históricas ni se presentan duraciones estimadas como horas efectivamente trabajadas.
