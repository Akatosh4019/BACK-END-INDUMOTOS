# Cuentas y consulta offline — 23/11/2026

## Ramas

App: feature/ismael-offline-cuentas, desde c74cbe4. Backend: feature/ismael-cuentas-presentacion, desde f551cce. No se modifica main ni se reescribe el historial. Los cambios anteriores permanecen en sus ramas originales.

## Datos locales de presentación

Solo copia indumotos_s2_paridad_20261123. Respaldo completo previo de266483 bytes verificado con pg_restore --list; archivo privado fuera del repositorio. Hash y cambios exactos en evidencias/cuentas-20261123/resultado-cuentas-catalogo.json. No se publica el dump ni contraseñas/hashes de usuarios.

| Cuenta | Rol |
|---|---|
| admin.indumotos | Administrador |
| almacen.indumotos | Almacén |
| inventarios.indumotos | Inventarios |
| comercial.indumotos | Comercial |
| facturacion.indumotos | Facturación |
| ensamblaje.indumotos | Ensamblaje |

Las cinco cuentas operativas reemplazan nombres sintéticos conservando sus IDs. La cuenta administrativa de pruebas redundante queda renombrada administracion.indumotos e inactiva; no se borra porque tiene historial. Se conserva admin.indumotos. Contraseña académica común establecida localmente por instrucción del responsable; BCrypt con sal independiente por cuenta. La política general de alta de usuarios no se debilitó; no son credenciales de producción ni se incorporan al APK/Git.

Seis logins reales HTTP200; roles/permisos efectivos registrados en verificacion-cuentas-presentacion.json. El responsable aprobó habilitar solo consulta a Facturación. Se añadió INVENTARIO_CONSULTAR al rol en la copia local: inventario y ficha devuelven200; crear/editar ficha, registrar llegada, trasladar y crear cuenta devuelven403. Sin nuevos permisos de escritura. Evidencia: verificacion-facturacion.json. Comercial y Ensamblaje conservan sus permisos de solicitudes; no reciben inventario por cambiar el nombre de cuenta.

Catálogo sintético3: Suzuki / GN125 / Amarillo, mismo ID. Guías1–3: Prueba1, Prueba2, Prueba3. Sedes: Almacén central, Tienda principal, Almacén auxiliar, Tienda de apoyo. Nombres anteriores y evidencias históricas no se falsifican; chasis, motores, vínculos y movimientos se conservan. No se modifican migraciones ni bases históricas. La configuración de cuentas/permisos corresponde solo a esta copia local; clonar Git no recrea automáticamente sus datos.


No hay cambios Java ni migraciones en este bloque; se documenta la configuración de la copia local. El cambio móvil está en feature/ismael-offline-cuentas. Los archivos de evidencia citados pertenecen al repositorio móvil.
