# Datos para demostrar Indumotos

Docker Compose activa el perfil `demo` por defecto. Después de crear el administrador y aplicar Flyway, la aplicación carga:

- Una guía ficticia `GR-DEMO-VISUAL-10` confirmada, con 20 motos de prueba y ubicación inicial en un almacén de prueba.
- Dos guías ficticias pendientes: `GUIA-DEMO-001` con 2 motos y `GUIA-DEMO-002` con 3 motos. Cada una tiene una imagen inventada para practicar el registro y el OCR.
- Las combinaciones de catálogo usadas por las motos y las guías de prueba. Flyway también crea las ubicaciones de Puruchuco.

Las imágenes se generaron para esta demostración y están marcadas como ficticias. No representan una recepción, venta ni documento empresarial real. El OCR solo produce sugerencias: la persona debe revisar y corregir los datos antes de guardar.

La carga es idempotente: reiniciar Docker no duplica registros. No se incluyen cuentas, contraseñas, tokens, solicitudes ni fotos de las guías reales de la base local. El administrador se crea con las variables de `.env`. Para iniciar sin estos datos, establece `SPRING_PROFILES_ACTIVE=default` en `.env` antes de `docker compose up -d --build api`.
