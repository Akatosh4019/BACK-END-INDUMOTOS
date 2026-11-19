# Backend Indumotos

Los cambios del equipo DAM se desarrollan de forma incremental en la rama `DAM`. La guía de comprobación del incremento de base logística está en `docs/pruebas/GUIA_VISUAL_BASE_LOGISTICA_DAM.md`.

Backend compartido por la web y la app móvil de Indumotos. Usa Java 21, Spring Boot, PostgreSQL y Flyway. V1–V5 crean cuentas, roles, permisos, llegadas y fichas; V6 impide chasis completos duplicados y V7 almacena los hashes de tokens revocados. Permite crear cuentas, iniciar y cerrar sesión con JWT, registrar llegadas y fichas, y asignar roles y excepciones individuales. La web está implementada; Android se desarrolla por separado.

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

Para compilar y arrancar desde el código fuente dentro de Docker, prepara el
`.env` local del paso 2 y ejecuta desde la raíz del repositorio:

```powershell
docker compose -p indumotos-sprint2-local -f compose.yaml -f compose.local.yaml up --build -d
curl.exe http://127.0.0.1:8080/api/health
```

El archivo de configuración adicional compila con Maven y Java 21. Para el
emulador Android con la URL predeterminada de la app, conecta ambos puertos con
`adb -s <serial-del-emulador> reverse tcp:8089 tcp:8080`.
La base local usa un volumen persistente. Las credenciales y los registros
sintéticos usados para verificar HU-05 pertenecen al entorno local.

1. Iniciar Docker Desktop.
2. Copiar `.env.example` a `.env`; cambiar `DB_PASSWORD` y `ADMIN_PASSWORD`, definir `ADMIN_USERNAME` y generar `JWT_SECRET_BASE64` a partir de 32 bytes aleatorios. `.env` está excluido de Git.
3. Ejecutar `mvn package` y después `docker compose up --build -d` desde esta carpeta.
4. Visitar `http://localhost:8080/api/health`. Si responde `{"status":"ok"}`, la API arrancó después de aplicar Flyway.
5. Para revisar las tablas: `docker compose exec db psql -U indumotos -d indumotos -c "\dt"` (sustituir el usuario si se cambió en `.env`).

Para conectarse desde DBeaver: PostgreSQL, host `127.0.0.1`, puerto `5433`, base `indumotos`, usuario y contraseña del `.env` local. El puerto solo se publica en la propia computadora.

El volumen de PostgreSQL conserva los datos entre reinicios. No ejecutar `docker compose down -v` si quieres conservarlos.

### Sin Docker

Instalar Java 21 o posterior, Maven y PostgreSQL. Crear una base vacía `indumotos` y un usuario con permisos sobre ella; configurar `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` y `JWT_SECRET_BASE64` como variables de entorno y ejecutar `mvn spring-boot:run`. Spring no lee `.env` por sí solo fuera de Docker Compose. La aplicación crea las **tablas**, no el servidor ni la base vacía.

No publicar contraseñas ni tokens en Git. `usuario.clave_hash` almacena BCrypt, nunca texto plano. HU-15-T05 ya aplica permisos por acción desde la base; el expediente técnico y las instrucciones de captura están en `docs/evidencias/HU-15-T05/resultado-pruebas.md`.

## Seguridad implementada y pendiente

Tomar del backend de Pharmamobil la separación de configuración de seguridad, autenticación, filtros, controladores, servicios y pruebas; no copiar sus cuentas de demostración, claves, permisos de farmacia ni rutas de productos/ventas. Para Indumotos:

1. HU-14: solo un administrador autorizado crea cuentas; contraseña con hash adaptativo; validación de usuario único y datos obligatorios. No habilitar autorregistro público.
2. HU-15: permisos del rol desde la base de datos, con excepciones individuales `PERMITIR`/`DENEGAR`; `DENEGAR` prevalece. La API relee los permisos vigentes en cada solicitud y responde 403 ante una cuenta autenticada sin permiso. `GET /api/v1/usuarios/{id}/permisos` consulta accesos; `PUT /api/v1/usuarios/{id}/rol` cambia el rol; `PUT` y `DELETE /api/v1/usuarios/{id}/permisos/{codigo}` guardan y retiran excepciones. Estas rutas requieren `PERMISOS_ASIGNAR`. Los permisos administrativos `USUARIOS_CREAR` y `PERMISOS_ASIGNAR` no pueden concederse por excepción a un rol no administrador.
3. HU-26: `POST /api/v1/auth/login` devuelve un JWT Bearer de 900 segundos. `POST /api/v1/auth/logout` requiere ese Bearer, devuelve 204 y revoca el token en el servidor. Toda solicitud protegida con ese mismo token devuelve 401, incluso después de reiniciar la API. Android debe llamar al endpoint antes de borrar su token local. HTTP Basic está deshabilitado.
4. Probar al menos acceso sin sesión (401), con sesión sin permiso (403), cuenta inactiva y denegación individual. La configuración inicial de Spring Security no sustituye estas pruebas.

La colección general para Postman está en `docs/postman/Indumotos-Sprint-1.postman_collection.json`; la secuencia de login, logout y reutilización del token está en `docs/postman/HU-26-T06-ciclo-sesion.postman_collection.json`. Ejecutarlas con cuentas ficticias y sin exportar credenciales ni tokens. El administrador no registra llegadas ni fichas por defecto según la matriz inicial.

HU-04-T05 añade `GET /api/v1/llegadas/{id}/detalle` (requiere `INVENTARIO_CONSULTAR`). Devuelve la llegada, sus fichas vinculadas por `id_llegada`, `cantidadFichas` y `diferenciaInformativa` = cantidad recibida menos fichas vinculadas. La diferencia es solo informativa; no aprueba la recepción ni modifica stock. Si la llegada no existe devuelve 404; si existe sin fichas devuelve una lista vacía y 200.

HU-02-T05 aplica unicidad a `upper(btrim(chasis_completo))`. `POST /api/v1/motocicletas` devuelve 409 cuando existe ese chasis. La respuesta incluye `fichaExistenteId` solo si la cuenta también tiene `INVENTARIO_CONSULTAR`; puede consultar la ficha mediante `GET /api/v1/motocicletas/{id}`. Las dos fichas repetidas de demostración previas a V6 se conservaron en `motocicleta_duplicado_archivo`.

HU-01 y HU-03 comparten `GET /api/v1/motocicletas` con permiso `INVENTARIO_CONSULTAR`. Los parámetros opcionales `marca`, `modelo` y `color` se combinan con AND; `chasis` busca por chasis completo o por los últimos cuatro dígitos cuando tiene exactamente cuatro. `pagina` comienza en 0 y `tamano` admite 1–100. La respuesta contiene resultados resumidos, total y páginas; la ficha completa se consulta mediante `GET /api/v1/motocicletas/{id}`.
