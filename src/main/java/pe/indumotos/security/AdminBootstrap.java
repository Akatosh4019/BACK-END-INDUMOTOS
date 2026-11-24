package pe.indumotos.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminBootstrap(JdbcTemplate jdbc, PasswordEncoder encoder,
        @Value("${ADMIN_USERNAME:}") String username,
        @Value("${ADMIN_PASSWORD:}") String password) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.length() < 12)
            throw new IllegalStateException("Configurar ADMIN_USERNAME y ADMIN_PASSWORD (mínimo 12 caracteres)");
        // No recrear al administrador inicial si luego cambia su nombre de usuario.
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM usuario u JOIN rol r ON r.id_rol=u.id_rol " +
            "WHERE r.codigo='ADMIN'", Integer.class);
        if (count != null && count > 0) return;
        jdbc.update("INSERT INTO usuario (id_rol, nombre_completo, nombre_usuario, clave_hash) " +
            "SELECT id_rol, 'Administrador inicial', ?, ? FROM rol WHERE codigo = 'ADMIN'",
            username, encoder.encode(password));
    }
}
