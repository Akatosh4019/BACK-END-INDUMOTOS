# Comprobación técnica local — 19/11/2026

- `GET http://127.0.0.1:8080/api/health`: `{"status":"ok"}`.
- Inicio de sesión con el administrador configurado en `.env`: exitoso. No se guardó la contraseña ni el token.
- Diez consultas autenticadas de inventario, una para cada chasis `IND-DEMO-S2-0001` a `IND-DEMO-S2-0010`: cada consulta devolvió exactamente un elemento.
- `GET http://127.0.0.1:4200/api/health`: `{"status":"ok"}` a través de web y gateway.

Estas son comprobaciones HTTP realizadas en el equipo; las capturas visuales de navegación corresponden a la prueba de usuario.
