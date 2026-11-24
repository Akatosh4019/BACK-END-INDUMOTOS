package pe.indumotos.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.CuentaRepository;

@Repository
public class JdbcCuentaRepository implements CuentaRepository {
    private static final String CUENTA = "SELECT u.id_usuario,u.nombre_completo,u.nombre_usuario,r.codigo,r.nombre," +
        "u.estado,ti.nombre,u.creado_en,u.actualizado_en FROM usuario u JOIN rol r ON r.id_rol=u.id_rol " +
        "LEFT JOIN usuario_tienda ut ON ut.id_usuario=u.id_usuario " +
        "LEFT JOIN ubicacion ti ON ti.id_ubicacion=ut.id_tienda WHERE u.id_usuario=?";
    private final JdbcTemplate jdbc;

    public JdbcCuentaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Cuenta buscar(long id) {
        return jdbc.query(CUENTA, rs -> rs.next() ? cuenta(rs) : null, id);
    }

    @Override
    public Cuenta bloquear(long id) {
        return jdbc.query(CUENTA + " FOR UPDATE OF u", rs -> rs.next() ? cuenta(rs) : null, id);
    }

    @Override
    public Long idActivoPorNombre(String nombreUsuario) {
        return jdbc.query("SELECT id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO'",
            rs -> rs.next() ? rs.getLong(1) : null, nombreUsuario);
    }

    @Override
    public long administradoresActivos() {
        Long total = jdbc.queryForObject("SELECT count(*) FROM (SELECT u.id_usuario FROM usuario u " +
            "JOIN rol r ON r.id_rol=u.id_rol WHERE r.codigo='ADMIN' AND u.estado='ACTIVO' FOR UPDATE OF u) a",
            Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public void cambiarEstado(long id, String estado) {
        jdbc.update("UPDATE usuario SET estado=?, actualizado_en=CURRENT_TIMESTAMP, " +
            "sesiones_validas_desde=date_trunc('second', CURRENT_TIMESTAMP), " +
            "sesion_version=sesion_version+1 WHERE id_usuario=?", estado, id);
    }

    @Override
    public void registrarEvento(long id, String estadoAnterior, String estadoNuevo, String motivo, long actorId) {
        jdbc.update("INSERT INTO usuario_estado_evento(id_usuario,estado_anterior,estado_nuevo,motivo,id_actor) " +
            "VALUES (?,?,?,?,?)", id, estadoAnterior, estadoNuevo, motivo, actorId);
    }

    @Override
    public List<EventoEstado> historial(long id) {
        return jdbc.query("SELECT e.estado_anterior,e.estado_nuevo,e.motivo,a.nombre_usuario,e.registrado_en " +
            "FROM usuario_estado_evento e JOIN usuario a ON a.id_usuario=e.id_actor " +
            "WHERE e.id_usuario=? ORDER BY e.id_evento DESC",
            (rs, n) -> new EventoEstado(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getObject(5, OffsetDateTime.class)), id);
    }

    private static Cuenta cuenta(ResultSet rs) throws SQLException {
        return new Cuenta(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
            rs.getString(6), rs.getString(7), rs.getObject(8, OffsetDateTime.class),
            rs.getObject(9, OffsetDateTime.class));
    }
}
