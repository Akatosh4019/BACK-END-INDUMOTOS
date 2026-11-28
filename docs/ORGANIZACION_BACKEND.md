# Organización del backend

La aplicación Spring Boot comienza en `BackendIndumotosApplication.java`.
Dentro de `pe.indumotos`, las clases se ordenan por capa y por área de trabajo.

| Capa | Función |
| --- | --- |
| `presentation` | Recibe peticiones HTTP y devuelve respuestas de la API. |
| `application` | Valida datos y coordina las operaciones del negocio. |
| `domain` | Representa los datos principales, como motocicleta y llegada. |
| `infrastructure` | Ejecuta las consultas que guardan o leen información en PostgreSQL. |
| `security` | Configura JWT, contraseñas y reglas de acceso. |
| `config` | Inicializa datos de demostración cuando corresponde. |

Dentro de `presentation`, `application` e `infrastructure`, las subcarpetas
`seguridad`, `trabajadores`, `catalogo`, `inventario`, `llegadas`,
`solicitudes`, `ubicaciones` y `ventas` agrupan archivos de la misma área.
`presentation/sistema` contiene la comprobación de salud de la API.

Ejemplo: `presentation/llegadas/GuiaLlegadaController.java` recibe una guía;
`application/llegadas/RegistrarGuiaLlegada.java` la valida y coordina;
`infrastructure/llegadas/JdbcGuiaLlegadaRepository.java` guarda sus fotos.

Las migraciones de Flyway permanecen en `src/main/resources/db/migration`.
Las pruebas reflejan las mismas subcarpetas en `src/test/java`.
