# Ajustes para la demostración

App0.3.5/código12, fuente f9c24db. Backend0e84511. Ramas feature/ismael-offline-cuentas y feature/ismael-cuentas-presentacion; main intacto.

Traslados: lista y recorrido separados; detalle visible arriba con Volver a motocicletas, carga y reintento. ViewModel independiente de solicitudes. Se observó en Samsung el recorrido con ubicación e historial. La captura automatizada coincidió con cambios de pantalla del usuario y no se publica como evidencia concluyente.

Inventario: espacio de carga fijo para evitar salto de contenido; bloqueo de pulsaciones repetidas al cargar ficha. Compilado; confirmación perceptiva del parpadeo y apertura final de ficha pendiente de revisión manual, no se declara aprobada.

Recuperación interna: Olvidé mi contraseña explica el procedimiento sin afirmar que envía correos. Administración puede restablecer otra cuenta activa desde Personal registrado tras verificar identidad y reintroducir su propia contraseña. Nueva contraseña12–100 caracteres, BCrypt, máximo5 intentos por10 minutos por administrador. Se invalidan tokens online anteriores al comparar una marca de credencial; las sesiones previas al despliegue requieren login nuevamente. Caché offline no puede revocarse remotamente mientras está sin conexión: mantiene límite24h y desbloqueo del dispositivo. No se simula recuperación por correo ni preguntas secretas.

Verificación:53 pruebas Android JVM aprobadas,8 backend específicas aprobadas;14 comprobaciones HTTP reales aprobadas (incluye acceso sin token401, rol operativo403, contraseña de administrador incorrecta403, validaciones400, reset204, token anterior401, clave anterior401 y login nuevo). Cuenta transitoria eliminada al terminar; cuentas operativas y sus contraseñas no cambiaron. Respaldo completo privado anterior al despliegue verificado. Sin nuevas migraciones, volumen o esquema.

## Docker para el examen

Iniciar solamente indumotos-distribuidas-db-1 y indumotos-paridad-api. Los contenedores pre-offline, antes-recuperacion y recuperacion-inicial son respaldos detenidos con reinicio automático desactivado; no iniciarlos. API actual indumotos-paridad-ocr:0e84511. Puerto API127.0.0.1:8090 y PostgreSQL127.0.0.1:55432; red Docker privada indumotos-paridad.

Desde la carpeta del aplicativo, en PowerShell:

    ./scripts/Iniciar-Entorno-Paridad.ps1 -Serial RFGL11XYCTD

El script inicia los dos contenedores, restablece teléfono8089→PC8090 y espera HTTP200. Reconectar USB/reiniciar ADB requiere volver a crear el túnel. No ejecutar down -v ni iniciar contenedores históricos/Oracle para esta demostración.

## Seguridad y GitHub

El puerto ligado a127.0.0.1 no es un despliegue público. GitHub aloja código y documentación; el repositorio móvil es público, lo que no convierte la API local en un servicio público. No se cambió la visibilidad ni se afirma que secret scanning/push protection estén activados. Credenciales, JWT, dumps y archivos privados se excluyen; revisar staged antes de publicar. Commits cortos y directos; sin reescribir historia ni fusionar a main.

Fuentes oficiales consultadas: https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html ; https://docs.docker.com/engine/network/port-publishing/ ; https://docs.github.com/en/code-security/how-tos/secure-your-secrets/prevent-future-leaks/enable-push-protection . No hay despliegue remoto ni firewall abierto.
