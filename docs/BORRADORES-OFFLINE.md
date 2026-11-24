# Borradores offline: control de concurrencia

Rama feature/sprint2-offline-borradores desde main 5b37b9b. Sin migraciones ni cambios en cuentas/datos existentes.

GET /api/v1/llegadas/{id}/registro añade `version`, hash SHA-256 del contenido y estado del registro. PUT /registro/borradores/{posicion} acepta opcionalmente `If-Match: "<version>"`. Compara dentro de la misma transacción y después del bloqueo de la llegada; si cambió devuelve HTTP 412 antes de escribir. El móvil conserva el pendiente y exige revisión. La web existente sin cabecera mantiene su contrato anterior.

No se basa en reloj del teléfono, no crea stock al guardar y no reintenta finalizaciones. El cliente offline debe exigir versión antes de enviar, refrescar permisos y distinguir red/401/403/409/412.

RegistroGuiaVersionTest: dos pruebas aprobadas para versión estable y cambios de contenido/finalización. Prueba HTTP concurrente real aún pendiente al crear este checkpoint. No presentar las pruebas unitarias como esa validación integral.
