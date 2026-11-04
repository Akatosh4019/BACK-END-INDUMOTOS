# HU-26-T02 — validación de credenciales y emisión de token

Implementado el 03/11/2026 en el backend local. El endpoint `POST /api/v1/auth/login` recibe `nombreUsuario` y `clave`, valida contra el hash BCrypt de una cuenta activa y emite un JWT firmado HS256 con expiración de 900 segundos. El token no contiene la contraseña. Los endpoints protegidos aceptan `Authorization: Bearer <token>`; HTTP Basic se deshabilitó.

Verificación realizada sin imprimir claves ni tokens:

| Caso | Resultado |
| --- | --- |
| Login administrador válido | HTTP 200, `tokenType=Bearer`, `expiresIn=900`, token presente |
| Token administrador en creación de usuario | HTTP 201 |
| Clave incorrecta | HTTP 401 |
| Token falso | HTTP 401 |
| Basic Auth antiguo | HTTP 401 |
| `mvn -q clean verify` | 7 pruebas pasadas, incluidas firma/expiración del JWT y rechazo de credenciales incorrectas |

Colección actualizada: `../postman/Indumotos-Sprint-1.postman_collection.json`. Ejecutar primero «Login administrador - Obtener token (200)». Las capturas de Postman aún no se han recibido. Esta tarea no implementa el botón ni la revocación de cierre de sesión; HU-26 completa sigue pendiente. La API está limitada a localhost; antes de usarla fuera del equipo se requiere HTTPS y revisar el manejo/almacenamiento del token en web y Android.
