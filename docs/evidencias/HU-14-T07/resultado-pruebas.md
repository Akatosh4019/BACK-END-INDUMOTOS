# HU-14-T07 — resultado de pruebas de creación de cuentas

Fecha: 01/11/2026. Alcance: comprobar registro correcto, duplicados, datos faltantes e intentos de acceso no autorizado. La prueba usa el backend local, Postman y DBeaver.

| Caso | Resultado | Evidencia |
| --- | --- | --- |
| Administrador crea una cuenta de Almacén | 201; respuesta sin contraseña | `HU-14-T05-01-creacion-201.png` |
| Nombre de usuario repetido | 409 | `HU-14-T05-02-duplicado-409.png` |
| Solicitud sin credenciales | 401 | `HU-14-T05-03-sin-autenticacion-401.png` |
| Cuenta de Almacén intenta crear otra cuenta | 403 | `HU-14-T05-04-rol-almacen-403.png` |
| Nombre completo vacío | 400; identifica `nombreCompleto` | `05-nombre-vacio-400.png` |
| Contraseña corta | 400; identifica `clave` sin exponer su valor | `06-clave-corta-400.png` |
| Rol inexistente | 400 | `07-rol-inexistente-400.png` |
| Contraseña almacenada como hash | Prefijo BCrypt `$2a$`, 60 caracteres, `es_bcrypt = true` | `08-hash-bcrypt-dbeaver.png` |

Además, `mvn -q clean verify` pasó con la prueba de salud y cuatro pruebas automatizadas de validación de datos. Las capturas de Postman fueron aportadas por Roberto y la captura DBeaver adicional muestra la consulta y el resultado sin revelar el hash completo. Todos los casos previstos para HU-14-T07 tienen evidencia favorable. **T07 puede pasar a Done**; esto no cierra por sí solo HU-14 ni demuestra que exista el formulario web/móvil.

Esta carpeta conserva copias de las siete capturas de T05/T06 para que el expediente T07 sea autónomo; los originales permanecen en sus carpetas respectivas.
