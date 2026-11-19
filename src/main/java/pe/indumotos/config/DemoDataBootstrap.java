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
    }
}
