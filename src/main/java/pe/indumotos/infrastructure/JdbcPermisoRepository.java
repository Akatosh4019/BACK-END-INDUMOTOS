package pe.indumotos.infrastructure;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.PermisoRepository;

@Repository
public class JdbcPermisoRepository implements PermisoRepository {
    private final JdbcTemplate jdbc;

    public JdbcPermisoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Cuenta buscarPorNombre(String nombreUsuario) {
        return jdbc.query("SELECT u.id_usuario, u.nombre_usuario, r.codigo, u.estado, r.estado " +
            "FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.nombre_usuario=?",
            rs -> rs.next() ? new Cuenta(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5)) : null, nombreUsuario);
    }

    @Override
    public Cuenta buscarPorId(long id) {
        return jdbc.query("SELECT u.id_usuario, u.nombre_usuario, r.codigo, u.estado, r.estado " +
            "FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=?",
            rs -> rs.next() ? new Cuenta(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5)) : null, id);
    }

    @Override
    public boolean existeRolActivo(String codigo) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM rol WHERE codigo=? AND estado='ACTIVO')", Boolean.class, codigo));
    }

    @Override
    public boolean existePermiso(String codigo) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM permiso WHERE codigo=?)", Boolean.class, codigo));
    }

    @Override
    public List<Asignacion> asignaciones(long usuarioId) {
        return jdbc.query("SELECT p.codigo, EXISTS(SELECT 1 FROM rol_permiso rp " +
            "JOIN usuario u ON u.id_rol=rp.id_rol WHERE u.id_usuario=? AND rp.id_permiso=p.id_permiso), " +
            "up.decision FROM permiso p LEFT JOIN usuario_permiso up " +
            "ON up.id_permiso=p.id_permiso AND up.id_usuario=? ORDER BY p.codigo",
            (rs, row) -> new Asignacion(rs.getString(1), rs.getBoolean(2), rs.getString(3)),
            usuarioId, usuarioId);
    }

    @Override
    public void cambiarRol(long usuarioId, String rolCodigo) {
        jdbc.update("UPDATE usuario SET id_rol=(SELECT id_rol FROM rol WHERE codigo=? AND estado='ACTIVO'), " +
            "actualizado_en=CURRENT_TIMESTAMP WHERE id_usuario=?", rolCodigo, usuarioId);
    }

    @Override
    public void guardarExcepcion(long usuarioId, String codigo, String decision) {
        jdbc.update("INSERT INTO usuario_permiso(id_usuario,id_permiso,decision) " +
            "SELECT ?,id_permiso,? FROM permiso WHERE codigo=? " +
            "ON CONFLICT(id_usuario,id_permiso) DO UPDATE SET decision=EXCLUDED.decision",
            usuarioId, decision, codigo);
    }

    @Override
    public void retirarExcepcion(long usuarioId, String codigo) {
        jdbc.update("DELETE FROM usuario_permiso WHERE id_usuario=? AND id_permiso=" +
            "(SELECT id_permiso FROM permiso WHERE codigo=?)", usuarioId, codigo);
    }
}
