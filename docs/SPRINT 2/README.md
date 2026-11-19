# Sprint 2 · Indumotos

Trabajo realizado sobre la rama `sprint-2` de los repositorios existentes de backend y web. No se han modificado Android ni modelos manuales.

| Historia | Entrega verificable | Estado |
| --- | --- | --- |
| HU-05 | Catálogo de sedes, registro de ubicación inicial, consulta de ubicación e historial; pantalla web | Código y pruebas disponibles |
| HU-06 | Solicitudes con líneas, destino y estado inicial; pantalla de creación y consulta | Código y pruebas disponibles |
| HU-07 | Historial de eventos y transiciones controladas; pantalla de seguimiento | Código y pruebas disponibles |
| HU-08 | Búsqueda, asignación exclusiva y retiro de unidades; controles web | Código y pruebas disponibles |
| HU-09 | Traslado entre sedes con origen, destino y usuario; pantalla compartida de ubicaciones | Código y pruebas disponibles |
| HU-27 | Anulación con motivo y evento conservado | Código disponible; pendiente confirmar con Product Owner el estado límite para anular |

## Evidencia de ejecución

- Backend: `mvn test` con Java 21; **62 pruebas, 0 fallos, BUILD SUCCESS**. Las pruebas cubren reglas de ubicación, solicitudes, seguimiento, asignación y anulación. No sustituyen una ejecución HTTP integral.
- Frontend Angular: `ng build` concluye correctamente; `ng test --watch=false`: **6 pruebas aprobadas**.
- Base de datos: migraciones V8 a V15 aplicadas progresivamente sobre PostgreSQL 18 aislado en `localhost:55440`, base `s2_hu06_verify_live`; V14 y V15 aplicadas con `psql -v ON_ERROR_STOP=1`. Las consultas confirmaron la creación de los permisos `TRASLADOS_REGISTRAR` y `SOLICITUDES_ANULAR`. Esta base es de verificación y no contiene registros reales del proyecto.
- Intento de `mvn package` offline: las 62 pruebas se ejecutan, pero el empaquetado termina con **BUILD FAILURE** por dependencias de plugins ausentes en el caché Maven, entre ellas `org.tukaani:xz` y `zstd-jni`.
- Intento de `mvn test` offline en gateway: **bloqueado** por `spring-cloud-dependencies:2025.1.3` ausente en el caché Maven. No hay prueba HTTP a través del gateway.

## Pendiente para cerrar el Sprint

1. Descargar las dependencias Maven del empaquetado y gateway en un equipo con acceso a Maven Central; ejecutar `mvn package` en backend y `mvn test` en `gatewayIndumotos`.
2. Arrancar backend con PostgreSQL y gateway; comprobar login JWT, permisos, escrituras y lectura reales a través de HTTP, además de regresión del Sprint 1.
3. Solicitar al Product Owner la regla definitiva de HU-27. La implementación actual permite anular solamente solicitudes `PENDIENTE` y exige motivo.
4. Tomar capturas reales desde la interfaz cuando Roberto haga las pruebas visuales. No se han fabricado capturas.
5. Revisar y subir ambos repositorios remotos cuando se habilite el acceso al Git remoto.

Los commits registran las fechas reales de ejecución. No se asignan horas históricas ni se presentan duraciones estimadas como horas efectivamente trabajadas.
