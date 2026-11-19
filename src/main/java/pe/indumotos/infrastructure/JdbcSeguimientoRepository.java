package pe.indumotos.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.SeguimientoRepository;

@Repository
public class JdbcSeguimientoRepository implements SeguimientoRepository {
    private final JdbcTemplate jdbc;
    public JdbcSeguimientoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean bloquearSolicitud(long id) {
        return !jdbc.query("SELECT id_solicitud FROM solicitud_ensamblaje WHERE id_solicitud=? FOR UPDATE",
            (rs,n) -> rs.getLong(1), id).isEmpty();
    }

    @Override
    public boolean existeSolicitud(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM solicitud_ensamblaje WHERE id_solicitud=?)",Boolean.class,id));
    }

    @Override
    public Evento estado(long id) {
        return jdbc.query("SELECT e.id_evento,e.estado,u.nombre_usuario,e.registrado_en,e.motivo " +
            "FROM solicitud_ensamblaje_estado_actual e JOIN usuario u ON u.id_usuario=e.id_registrador " +
            "WHERE e.id_solicitud=?",this::mapear,id).stream().findFirst().orElse(null);
    }

    @Override
    public Evento agregar(long id, String nuevoEstado, String motivo, String usuario) {
        Long evento = jdbc.queryForObject("INSERT INTO solicitud_ensamblaje_evento(id_solicitud,estado,id_registrador,motivo) " +
            "SELECT ?, ?, id_usuario, ? FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO' RETURNING id_evento",
            Long.class,id,nuevoEstado,motivo,usuario);
        jdbc.update("UPDATE solicitud_ensamblaje SET version=version+1 WHERE id_solicitud=?",id);
        return jdbc.queryForObject("SELECT e.id_evento,e.estado,u.nombre_usuario,e.registrado_en,e.motivo " +
            "FROM solicitud_ensamblaje_evento e JOIN usuario u ON u.id_usuario=e.id_registrador WHERE e.id_evento=?",
            this::mapear,evento);
    }

    @Override
    public List<Evento> historial(long id) {
        return jdbc.query("SELECT e.id_evento,e.estado,u.nombre_usuario,e.registrado_en,e.motivo " +
            "FROM solicitud_ensamblaje_evento e JOIN usuario u ON u.id_usuario=e.id_registrador " +
            "WHERE e.id_solicitud=? ORDER BY e.id_evento",this::mapear,id);
    }

    private Evento mapear(ResultSet rs,int row) throws SQLException {
        return new Evento(rs.getLong("id_evento"),rs.getString("estado"),rs.getString("nombre_usuario"),
            rs.getObject("registrado_en",OffsetDateTime.class),rs.getString("motivo"));
    }
}
