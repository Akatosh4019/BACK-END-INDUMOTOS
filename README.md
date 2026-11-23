# Backend Indumotos

API compartida por la web de Indumotos. Implementa los incrementos existentes de Sprint 1 y Sprint 2 en la rama `sprint-2` con Java 21, Spring Boot, PostgreSQL, Flyway y Docker.

**Empieza por [la guía del sistema implementado](docs/SISTEMA_IMPLEMENTADO.md).** Explica arquitectura, roles, permisos, llegada → borradores → fichas, solicitudes, ubicaciones, API, pruebas y trabajo pendiente. El [expediente original de Sprint 2](docs/SPRINT%202/README.md) y las carpetas de evidencias conservan el contexto de cada historia; algunas notas históricas tienen estados de prueba anteriores.

## Iniciar con Docker

1. Copia `.env.example` a `.env` y configura credenciales propias y `JWT_SECRET_BASE64`. Nunca subas `.env` al repositorio.
2. Desde esta carpeta ejecuta `docker compose up -d --build api`. Docker inicia PostgreSQL, aplica las migraciones Flyway y levanta la API.
3. Comprueba `http://127.0.0.1:8080/api/health`; si falla, revisa `docker compose logs --tail=100 api`.

PostgreSQL usa el volumen `indumotos_pgdata`; no ejecutes `docker compose down -v` si necesitas conservar los datos. El puerto de la API y el de la base se publican solo en `127.0.0.1`.

## Pruebas

Con Java 21 y Maven: `mvn test`. La última suite observada en esta rama completó **81 pruebas** sin fallos y Flyway llegó a **V23** en la base local. La revisión visual por rol y la finalización de una guía real siguen pendientes de validación humana.

La web Angular y el gateway están en el repositorio `SISTEMA-WEB-INDUMOTOS` y se inician después del backend.
