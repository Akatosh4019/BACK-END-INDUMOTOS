# HU-14-T06 — validaciones y restricción de acceso

Implementado el 01/11/2026 en `UsuarioController` y `SecurityConfig`.

La solicitud de creación exige nombre completo, nombre de usuario de 3 a 80 caracteres con caracteres permitidos, contraseña de 12 a 100 caracteres y un rol existente y activo. Los errores de validación devuelven HTTP 400 con los nombres de los campos, sin repetir la contraseña. Solo ADMIN puede crear cuentas: sin autenticación se obtiene 401; con el rol ALMACEN, 403.

Verificación local: `mvn -q clean verify` pasó, incluidas cuatro pruebas de validación; con Docker se obtuvo 400 para nombre vacío, contraseña corta y rol inexistente. Las capturas 401 y 403 de HU-14-T05 también muestran la restricción de acceso. La colección `docs/postman/Indumotos-Sprint-1.postman_collection.json` contiene los casos 00 a 07.

Capturas de Postman aportadas por Roberto, guardadas en `HU-14-T06/`:

- `05-nombre-vacio-400.png`: HTTP 400 y campo `nombreCompleto`.
- `06-clave-corta-400.png`: HTTP 400 y campo `clave` (sin revelar el valor de la contraseña).
- `07-rol-inexistente-400.png`: HTTP 400 y mensaje de rol no válido o inactivo.

Las tres respuestas coinciden con las pruebas automatizadas y la comprobación HTTP local. Se puede marcar HU-14-T06 como Done por su alcance. No implica que la HU-14 completa ni su tarea T07 estén terminadas.
