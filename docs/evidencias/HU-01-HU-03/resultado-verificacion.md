# HU-01 y HU-03 — búsqueda y filtros para la web

Fecha: 05/11/2026. Entorno: API y PostgreSQL aislados con datos ficticios.

El recorrido reproducible `verificar-busqueda.cjs` pasó **18/18 casos HTTP**: alta de datos de prueba, dos coincidencias por los últimos cuatro dígitos, búsqueda por chasis completo, filtros combinados, ficha abierta desde resultados, lista vacía, parámetro inválido, 401 sin sesión y 403 sin permiso. La consulta devuelve resúmenes paginados y requiere `INVENTARIO_CONSULTAR`.

`mvn -q test` pasó **36 pruebas**, sin fallos ni errores. El frontend Angular compiló con `npm run build` y pasó **4 pruebas** con `npm test -- --watch=false`; estas cubren filtros, paginación, búsqueda completa y estado vacío. La comprobación visual en navegador corresponde al recorrido de presentación del equipo.

Para repetir el recorrido HTTP, levantar backend y PostgreSQL con datos de prueba, definir `ADMIN_USERNAME` y `ADMIN_PASSWORD` en el entorno y ejecutar `node docs/evidencias/HU-01-HU-03/verificar-busqueda.cjs`. No se guardan credenciales ni tokens en este repositorio.
