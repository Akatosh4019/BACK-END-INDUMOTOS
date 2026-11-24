package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

/** Usa un esquema temporal independiente; nunca modifica tablas del sistema operativo. */
@EnabledIfEnvironmentVariable(named="CATALOGO_TEST_DB_URL", matches=".+")
class CatalogoEstadoIntegrationTest {
    @Test
    void migracionConservaDatosYPermiteDeshabilitarYRehabilitar() {
        var base = new DriverManagerDataSource(System.getenv("CATALOGO_TEST_DB_URL"),
            System.getenv("CATALOGO_TEST_DB_USER"), System.getenv("CATALOGO_TEST_DB_PASSWORD"));
        var conexion = new JdbcTemplate(base);
        String esquema = "catalogo_test_" + UUID.randomUUID().toString().replace("-", "");
        conexion.execute("CREATE SCHEMA " + esquema);
        try {
            Flyway.configure().dataSource(base).schemas(esquema).defaultSchema(esquema)
                .target("31").load().migrate();
            String url = System.getenv("CATALOGO_TEST_DB_URL");
            var aislada = new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + esquema,
                System.getenv("CATALOGO_TEST_DB_USER"), System.getenv("CATALOGO_TEST_DB_PASSWORD"));
            var jdbc = new JdbcTemplate(aislada);
            jdbc.update("INSERT INTO catalogo_motocicleta(marca,modelo,color) VALUES('Honda','CB','Rojo')");
            Flyway.configure().dataSource(base).schemas(esquema).defaultSchema(esquema).load().migrate();
            var catalogo = new CatalogoMotocicletas(jdbc);
            var inicial = catalogo.administrar().getFirst();
            assertTrue(inicial.habilitado(), "La migración mantiene habilitados los registros anteriores");
            assertEquals(1, catalogo.listar().size());
            var deshabilitada = catalogo.cambiarEstado(inicial.id(), false);
            assertFalse(deshabilitada.habilitado());
            assertTrue(catalogo.listar().isEmpty(), "Las opciones operativas no incluyen deshabilitadas");
            assertEquals(1, catalogo.administrar().size(), "Deshabilitar nunca elimina el registro");
            assertEquals("Rojo", catalogo.administrar().getFirst().color());
            catalogo.cambiarEstado(inicial.id(), true);
            assertEquals(1, catalogo.listar().size());
            assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> catalogo.cambiarEstado(Long.MAX_VALUE, false)).getStatusCode().value());
        } finally {
            conexion.execute("DROP SCHEMA " + esquema + " CASCADE");
        }
    }
}
