# HU-14-T05 — evidencia de registro seguro (01/11/2026)

Capturas aportadas por Roberto y conservadas en esta misma carpeta:

- `HU-14-T05-01-creacion-201.png`: cuenta creada por administrador; respuesta sin contraseña.
- `HU-14-T05-02-duplicado-409.png`: rechazo de nombre de usuario repetido.
- `HU-14-T05-03-sin-autenticacion-401.png`: rechazo sin credenciales.
- `HU-14-T05-04-rol-almacen-403.png`: rechazo a un trabajador de Almacén que intenta crear cuentas.

Estas capturas acreditan las respuestas de la API. La verificación del hash se documenta abajo con consulta reproducible; aún no se adjuntó captura de DBeaver para ese punto.

Prueba local ejecutada con Docker Compose y PostgreSQL. Cuenta de prueba: `evidencia_sprint1` (rol ALMACEN); no usarla como cuenta real.

| Verificación | Resultado |
| --- | --- |
| Administrador crea la cuenta por `POST /api/v1/usuarios` | HTTP 201; respuesta con ID, nombre, usuario y rol, sin contraseña ni hash |
| Se repite el nombre de usuario | HTTP 409 |
| Se intenta crear sin autenticación | HTTP 401 |
| Se consulta `usuario.clave_hash` en PostgreSQL | Prefijo `$2a$`; valor diferente de la contraseña enviada |
| Compilación y pruebas | `mvn -q clean verify` completó con salida 0 |

Consulta para captura en DBeaver (solo muestra un prefijo, nunca el hash completo):

```sql
SELECT nombre_usuario, estado, LEFT(clave_hash, 4) AS prefijo_hash,
       clave_hash <> 'ClaveDePrueba_2026!' AS no_es_texto_plano
FROM usuario
WHERE nombre_usuario = 'evidencia_sprint1';
```

La autenticación actual de esta etapa usa HTTP Basic únicamente para la prueba local de la API. Antes de conectar web y móvil corresponde implementar autenticación con token, expiración y transporte HTTPS; no publicar Basic sobre HTTP. No se implementó aún toda la gestión de permisos de HU-15.
