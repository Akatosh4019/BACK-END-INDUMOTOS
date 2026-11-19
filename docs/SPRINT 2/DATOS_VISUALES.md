# Datos de prueba para el recorrido visual

Archivo: `datos_prueba_visual.sql`. Crea una llegada de 10 unidades, diez fichas con chasis `IND-DEMO-S2-0001` a `IND-DEMO-S2-0010` y tres ubicaciones de prueba. Las unidades empiezan en el almacén central. Al volver a ejecutarlo, no duplica esos registros.

Con Docker Desktop en ejecución, desde la carpeta `BACK-END-INDUMOTOS` ejecutar en PowerShell:

```powershell
Get-Content -LiteralPath '.\docs\SPRINT 2\datos_prueba_visual.sql' -Raw -Encoding UTF8 | docker compose exec -T db psql -U indumotos -d indumotos -v ON_ERROR_STOP=1
```

La última línea debe indicar `motocicletas_demo = 10`. Si se cambió `DB_USER` en `.env`, usar ese valor en `-U`. No se genera ninguna contraseña nueva; las fichas quedan asociadas al administrador local `admin`. Este conjunto se carga únicamente al ejecutar el comando y no forma parte de las migraciones de producción.

Para comprobarlo en la web, inicia sesión, abre Inventario y busca `IND-DEMO-S2-0001`, o filtra por marca `Honda`. Se puede consultar el detalle de la llegada usando la guía `GR-DEMO-VISUAL-10` en la base de datos.
