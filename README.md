# Backend Indumotos

Esta es la API y la base de datos del sistema Indumotos. El trabajo está en la rama `sprint-2` de Roberto Samuel Valencia Saavedra. Usa Java 21, Spring Boot, PostgreSQL, Flyway y Docker. La web y el gateway están en [SISTEMA-WEB-INDUMOTOS](https://github.com/Akatosh4019/SISTEMA-WEB-INDUMOTOS/tree/sprint-2).

## ¿Qué hace?

1. **Administración** crea cuentas y permisos, define marcas, modelos y colores, y registra sedes.
2. **Almacén** registra una llegada con los datos y fotos de su guía. La guía queda pendiente.
3. **Inventarios** revisa las sugerencias de lectura de las fotos, corrige los datos, guarda borradores de cada moto y finaliza la guía. Entonces las motos entran al inventario y la guía queda confirmada.
4. **Comercial** solicita motos del catálogo para una tienda. **Almacén** prepara chasis concretos y registra su salida. Comercial confirma la recepción; así cambia la ubicación confirmada de cada moto.
5. **Facturación** consulta la disponibilidad y puede reportar diferencias. **Inventarios** investiga y corrige esas diferencias con una fuente registrada.

La API comprueba el JWT, los permisos y las reglas de cada operación. Las fotos de guías y los borradores quedan en PostgreSQL, por lo que sobreviven a reinicios de contenedores. El OCR de Tesseract solo propone datos: una persona debe verificarlos. La base incluye migraciones Flyway **V1–V30**. Las tablas y API internas de ventas se conservan por compatibilidad, aunque **Modo tienda** y el registro de ventas se retiraron de la interfaz web de esta entrega. No hay cobro en línea.

Al iniciar Docker con el perfil `demo` se cargan automáticamente 20 motocicletas de prueba, un catálogo utilizable y dos guías **completamente ficticias** con imágenes para practicar el registro y el OCR. No son documentos comerciales. [Detalles de los datos de demostración](src/main/resources/db/demo/README.md).

**Aporte incorporado de Luciana Gutiérrez:** Administración puede consultar el detalle, permisos e historial de una cuenta, y desactivarla o reactivarla con motivo. El cambio de estado conserva los registros e invalida sesiones anteriores. La consulta de disponibilidad indica quién registró el estado, por qué una moto no se puede ofrecer y si tiene una diferencia pendiente.

## Iniciar en Docker

Desde la raíz de este repositorio:

```powershell
Copy-Item .env.example .env
# Configura las credenciales y JWT_SECRET_BASE64 en .env
docker compose up -d --build api
docker compose ps
```

Comprueba `http://127.0.0.1:8080/api/health`. Si hay un problema, ejecuta `docker compose logs --tail=100 api`. `compose.yaml` usa el perfil `demo` por defecto; establece `SPRING_PROFILES_ACTIVE=default` en `.env` para iniciar sin datos de ejemplo. PostgreSQL usa el volumen `indumotos_pgdata`; `docker compose down -v` lo eliminaría.

## Pruebas y alcance

Con Java 21 y Maven: `mvn test`. La última ejecución backend pasó **101 pruebas** y aplicó Flyway hasta **V30** en PostgreSQL local. La revisión visual por rol y una finalización de guía real autorizada siguen pendientes. Esto describe lo implementado; no declara terminado todo el producto.

Lee [cómo funciona el sistema](docs/SISTEMA_IMPLEMENTADO.md) para arquitectura, rutas, permisos y límites, y [el expediente de Sprint 2](docs/SPRINT%202/README.md) para las tareas y evidencias de ese sprint.
