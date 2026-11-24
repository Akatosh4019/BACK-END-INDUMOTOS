package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PerfilService {
    public record Perfil(String nombreCompleto, String nombreUsuario, String rolCodigo,
        String rolNombre, String estado, String tiendaNombre, String documento, String telefono, OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn) {}
    private record Cuenta(long id, String nombreUsuario, String claveHash) {}

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public PerfilService(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public Perfil consultar(String actor) {
        var perfiles = jdbc.query("SELECT u.nombre_completo,u.nombre_usuario,r.codigo,r.nombre,u.estado," +
            "t.nombre,u.documento,u.telefono,u.creado_en,u.actualizado_en FROM usuario u JOIN rol r ON r.id_rol=u.id_rol " +
            "LEFT JOIN usuario_tienda ut ON ut.id_usuario=u.id_usuario " +
            "LEFT JOIN ubicacion t ON t.id_ubicacion=ut.id_tienda " +
            "WHERE u.nombre_usuario=? AND u.estado='ACTIVO'",
            (rs, n) -> new Perfil(rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6),
                rs.getString(7), rs.getString(8), rs.getObject(9, OffsetDateTime.class),
                rs.getObject(10, OffsetDateTime.class)), actor);
        if (perfiles.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no disponible");
        return perfiles.getFirst();
    }

    @Transactional
    public Perfil cambiarUsuario(String actor, String nuevoNombre, String claveActual) {
        Cuenta cuenta = verificarClave(actor, claveActual);
        String nombre = nuevoNombre == null ? "" : nuevoNombre.trim().toLowerCase(Locale.ROOT);
        if (!nombre.matches("[a-z0-9._-]{3,80}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa de 3 a 80 letras, números, puntos, guiones o guiones bajos");
        if (nombre.equals(cuenta.nombreUsuario()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe un nombre de usuario distinto");
        try {
            jdbc.update("UPDATE usuario SET nombre_usuario=?, actualizado_en=CURRENT_TIMESTAMP, " +
                "sesion_version=sesion_version+1 WHERE id_usuario=?", nombre, cuenta.id());
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese nombre de usuario ya está ocupado");
        }
        return consultar(nombre);
    }

    @Transactional
    public void cambiarClave(String actor, String claveActual, String claveNueva) {
        Cuenta cuenta = verificarClave(actor, claveActual);
        if (claveNueva == null || claveNueva.length() < 12 || claveNueva.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña nueva debe tener entre 12 y 100 caracteres");
        if (encoder.matches(claveNueva, cuenta.claveHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña nueva debe ser diferente");
        jdbc.update("UPDATE usuario SET clave_hash=?, actualizado_en=CURRENT_TIMESTAMP, " +
            "sesion_version=sesion_version+1 WHERE id_usuario=?", encoder.encode(claveNueva), cuenta.id());
    }

    @Transactional
    public Perfil cambiarTelefono(String actor, String claveActual, String telefono) {
        Cuenta cuenta = verificarClave(actor, claveActual);
        String valor = telefono == null ? "" : telefono.trim();
        if (!valor.isEmpty() && !valor.matches("[+0-9 -]{6,30}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El teléfono debe tener entre 6 y 30 caracteres válidos");
        jdbc.update("UPDATE usuario SET telefono=?,actualizado_en=CURRENT_TIMESTAMP WHERE id_usuario=?",
            valor.isEmpty() ? null : valor, cuenta.id());
        return consultar(actor);
    }

    private Cuenta verificarClave(String actor, String claveActual) {
        if (claveActual == null || claveActual.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe tu contraseña actual");
        var cuentas = jdbc.query("SELECT id_usuario,nombre_usuario,clave_hash FROM usuario " +
            "WHERE nombre_usuario=? AND estado='ACTIVO' FOR UPDATE",
            (rs, n) -> new Cuenta(rs.getLong(1), rs.getString(2), rs.getString(3)), actor);
        if (cuentas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no disponible");
        Cuenta cuenta = cuentas.getFirst();
        if (!encoder.matches(claveActual, cuenta.claveHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no coincide");
        return cuenta;
    }
}
