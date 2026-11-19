# Datos de prueba para el recorrido visual

La configuración local de Docker activa el perfil `demo`. Después de Flyway y de crear la cuenta administradora, el backend carga automáticamente una llegada de 20 unidades, veinte fichas con chasis `IND-DEMO-S2-0001` a `IND-DEMO-S2-0020` y tres ubicaciones de prueba. Las unidades empiezan en el almacén central. Al reiniciar, no duplica esos registros.

Desde la carpeta `BACK-END-INDUMOTOS` ejecutar:

```powershell
docker compose up --build -d
docker compose exec db psql -U indumotos -d indumotos -c "SELECT count(*) AS motocicletas_demo FROM motocicleta WHERE chasis_completo LIKE 'IND-DEMO-S2-%';"
```

El resultado debe indicar `motocicletas_demo = 20`. Si se cambió `DB_USER` en `.env`, usar ese valor en `-U`. No se genera ninguna contraseña nueva; las fichas quedan asociadas al administrador local configurado en `ADMIN_USERNAME`. Los datos se cargan únicamente con el perfil `demo` y no forman parte de las migraciones de producción.

Para comprobarlo en la web, inicia sesión, abre Inventario y busca `IND-DEMO-S2-0001`, o filtra por marca `Honda`. La lista de inventario permite probar la página 1 (registros 1–10) y la página 2 (11–20). Se puede consultar el detalle de la llegada usando la guía `GR-DEMO-VISUAL-10` en la base de datos.
