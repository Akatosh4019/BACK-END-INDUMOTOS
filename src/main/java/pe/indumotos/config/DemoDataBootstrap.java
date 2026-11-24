package pe.indumotos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.io.IOException;

/** Loads predictable sample data only for the local Docker demo profile. */
@Component
@Profile("demo")
@Order(1)
public class DemoDataBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final String adminUsername;

    public DemoDataBootstrap(JdbcTemplate jdbc, @Value("${ADMIN_USERNAME}") String adminUsername) {
        this.jdbc = jdbc;
        this.adminUsername = adminUsername;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            // Pass the configured username as data; never concatenate it into SQL.
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT set_config('indumotos.demo_admin', ?, false)")) {
                statement.setString(1, adminUsername);
                statement.execute();
            }
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/demo/datos_prueba_visual.sql"));
            return null;
        });
        cargarFoto("GUIA-DEMO-001", 1, "GUIA-DEMO-001.png");
        cargarFoto("GUIA-DEMO-002", 1, "GUIA-DEMO-002.png");
    }

    private void cargarFoto(String numeroGuia, int orden, String nombre) {
        try (var archivo = new ClassPathResource("db/demo/guias/" + nombre).getInputStream()) {
            byte[] contenido = archivo.readAllBytes();
            jdbc.update("INSERT INTO llegada_guia_imagen " +
                "(id_llegada,orden,nombre_archivo,tipo_mime,tamano,contenido) " +
                "SELECT l.id_llegada,?::smallint,?,'image/png',?,? FROM llegada l " +
                "WHERE l.id_llegada=(SELECT min(id_llegada) FROM llegada WHERE numero_guia=?) " +
                "ON CONFLICT (id_llegada,orden) DO NOTHING",
                orden, nombre, contenido.length, contenido, numeroGuia);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar la imagen de demostración de " + numeroGuia, ex);
        }
    }
}
