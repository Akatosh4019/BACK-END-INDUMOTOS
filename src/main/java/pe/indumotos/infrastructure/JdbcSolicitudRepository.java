package pe.indumotos.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.SolicitudRepository;

@Repository
public class JdbcSolicitudRepository implements SolicitudRepository {
    private final JdbcTemplate jdbc;
    public JdbcSolicitudRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean destinoActivo(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM ubicacion WHERE id_ubicacion = ? AND activo)", Boolean.class, id));
    }

    @Override
    public Solicitud guardar(long destinoId, String observacion, String usuario, List<NuevaLinea> lineas) {
        Long id = jdbc.queryForObject("INSERT INTO solicitud_ensamblaje(id_solicitante,id_destino,observacion) " +
            "SELECT id_usuario,?,? FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO' RETURNING id_solicitud",
            Long.class, destinoId, observacion, usuario);
        for (var linea : lineas)
            jdbc.update("INSERT INTO solicitud_ensamblaje_detalle(id_solicitud,marca,modelo,color,cantidad) " +
                "VALUES(?,?,?,?,?)", id, linea.marca(), linea.modelo(), linea.color(), linea.cantidad());
        jdbc.update("INSERT INTO solicitud_ensamblaje_evento(id_solicitud,estado,id_registrador) " +
            "SELECT ?, 'PENDIENTE', id_usuario FROM usuario WHERE nombre_usuario=?", id, usuario);
        return buscar(id);
    }

    @Override
    public Solicitud buscar(long id) {
        return jdbc.query("SELECT s.id_solicitud,u.nombre_usuario,s.id_destino,s.creada_en,s.observacion,e.estado " +
            "FROM solicitud_ensamblaje s JOIN usuario u ON u.id_usuario=s.id_solicitante " +
            "JOIN solicitud_ensamblaje_estado_actual e ON e.id_solicitud=s.id_solicitud " +
            "WHERE s.id_solicitud=?", (rs,n) -> mapear(rs), id).stream().findFirst().orElse(null);
    }

    @Override
    public List<Solicitud> listar(int limite, int desplazamiento) {
        return jdbc.query("SELECT s.id_solicitud,u.nombre_usuario,s.id_destino,s.creada_en,s.observacion,e.estado " +
            "FROM solicitud_ensamblaje s JOIN usuario u ON u.id_usuario=s.id_solicitante " +
            "JOIN solicitud_ensamblaje_estado_actual e ON e.id_solicitud=s.id_solicitud " +
            "ORDER BY s.id_solicitud DESC LIMIT ? OFFSET ?", (rs,n) -> mapear(rs), limite, desplazamiento);
    }

    private Solicitud mapear(ResultSet rs) throws SQLException {
        long id = rs.getLong("id_solicitud");
        var lineas = jdbc.query("SELECT id_detalle,marca,modelo,color,cantidad " +
            "FROM solicitud_ensamblaje_detalle WHERE id_solicitud=? ORDER BY id_detalle",
            (r,n) -> new Linea(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getInt(5)), id);
        return new Solicitud(id, rs.getString("nombre_usuario"),rs.getLong("id_destino"),
            rs.getObject("creada_en", OffsetDateTime.class), rs.getString("observacion"),rs.getString("estado"),lineas);
    }
}
