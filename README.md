# Backend Indumotos

Backend compartido por la futura web y app móvil de Indumotos. No contiene código ni datos de Pharmamobil. Usa Java 21, Spring Boot, PostgreSQL y Flyway. V1 crea las cinco tablas de usuarios, roles y permisos; V2 registra roles iniciales. Actualmente permite crear cuentas como administrador e iniciar sesión con un token JWT de 15 minutos. Todavía no existen los clientes web/móvil ni la política granular de HU-15.

**Verificación local 01/11/2026:** `mvn clean verify` pasó (1 prueba); Docker Compose levantó PostgreSQL y la API; `/api/health` respondió `{"status":"ok"}`; Flyway V1 figura aplicada una sola vez y existen las cinco tablas. La prueba SQL de `src/test/resources/sql/verificar_usuarios_roles_permisos.sql` confirmó inserciones relacionadas y rechazo de duplicados/FK inválida, con `ROLLBACK` final y cero usuarios de prueba persistidos. Esto verifica la estructura de HU-14-T03, no el registro de cuentas ni la seguridad funcional.

El diagrama editable del autor está en `docs/modelo/HU-14-T01-usuarios-roles-permisos.drawio`. La captura de DBeaver que muestra las tablas de HU-14-T03 está en `docs/evidencias/HU-14-T03-tablas-en-DBeaver.png`. La tabla `flyway_schema_history` es el historial técnico de migraciones, no una sexta tabla de negocio.

## Estructura prevista

- `domain`: reglas y entidades del negocio.
- `application`: casos de uso.
- `infrastructure`: persistencia y seguridad.
- `presentation`: API HTTP.
- `db/migration`: cambios de esquema versionados.

## Ejecución local

### Con Docker Desktop (base y API)

1. Iniciar Docker Desktop.
2. Copiar `.env.example` a `.env`; cambiar `DB_PASSWORD` y `ADMIN_PASSWORD`, definir `ADMIN_USERNAME` y generar `JWT_SECRET_BASE64` a partir de 32 bytes aleatorios. `.env` está excluido de Git.
3. Ejecutar `mvn package` y después `docker compose up --build -d` desde esta carpeta.
4. Visitar `http://localhost:8080/api/health`. Si responde `{"status":"ok"}`, la API arrancó después de aplicar Flyway.
5. Para revisar las tablas: `docker compose exec db psql -U indumotos -d indumotos -c "\dt"` (sustituir el usuario si se cambió en `.env`).

Para conectarse desde DBeaver: PostgreSQL, host `127.0.0.1`, puerto `5433`, base `indumotos`, usuario y contraseña del `.env` local. El puerto solo se publica en la propia computadora.

El volumen de PostgreSQL conserva los datos entre reinicios. No ejecutar `docker compose down -v` si quieres conservarlos.

### Sin Docker

Instalar Java 21 o posterior, Maven y PostgreSQL. Crear una base vacía `indumotos` y un usuario con permisos sobre ella; configurar `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` y `JWT_SECRET_BASE64` como variables de entorno y ejecutar `mvn spring-boot:run`. Spring no lee `.env` por sí solo fuera de Docker Compose. La aplicación crea las **tablas**, no el servidor ni la base vacía.

No publicar contraseñas ni tokens en Git. `usuario.clave_hash` almacena BCrypt, nunca texto plano. La política de permisos individuales aún debe implementarse y probarse en HU-15.

## Seguridad implementada y pendiente

Tomar del backend de Pharmamobil la separación de configuración de seguridad, autenticación, filtros, controladores, servicios y pruebas; no copiar sus cuentas de demostración, claves, permisos de farmacia ni rutas de productos/ventas. Para Indumotos:

1. HU-14: solo un administrador autorizado crea cuentas; contraseña con hash adaptativo; validación de usuario único y datos obligatorios. No habilitar autorregistro público.
2. HU-15: permisos del rol desde la base de datos, con excepciones individuales `PERMITIR`/`DENEGAR`; `DENEGAR` prevalece. Comprobar la autorización en el backend, no únicamente ocultando botones en la web.
3. HU-26: `POST /api/v1/auth/login` valida las credenciales, devuelve `accessToken` tipo Bearer con expiración de 900 segundos y acepta ese token en endpoints protegidos. HTTP Basic quedó deshabilitado. El cierre de sesión aún no está implementado: borrar el token del cliente no lo revoca en el servidor. Diseñar almacenamiento seguro y revocación antes de conectar web/móvil.
4. Probar al menos acceso sin sesión (401), con sesión sin permiso (403), cuenta inactiva y denegación individual. La configuración inicial de Spring Security no sustituye estas pruebas.

La colección vigente para Postman está en `docs/postman/Indumotos-Sprint-1.postman_collection.json`; ejecutar primero «Login administrador» y luego las solicitudes protegidas. No exponer la API fuera del entorno local antes de HTTPS, pruebas integrales y cierre de HU-14, HU-15 y HU-26.
