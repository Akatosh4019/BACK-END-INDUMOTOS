package pe.indumotos.infrastructure.solicitudes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.solicitudes.AsignacionRepository;

@Repository
public class JdbcAsignacionRepository implements AsignacionRepository {
    private final JdbcTemplate jdbc;
    public JdbcAsignacionRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Override
    public Linea bloquearLinea(long solicitudId,long detalleId) {
        // Bloquear la cabecera serializa el conteo y los cambios de todas sus líneas.
        if (jdbc.query("SELECT id_solicitud FROM solicitud_ensamblaje WHERE id_solicitud=? FOR UPDATE",
            (rs,n) -> rs.getLong(1),solicitudId).isEmpty()) return null;
        return jdbc.query("SELECT d.id_solicitud,d.id_detalle,d.marca,d.modelo,d.color,d.cantidad,e.estado," +
            "(SELECT count(*) FROM solicitud_ensamblaje_unidad a WHERE a.id_detalle=d.id_detalle AND a.retirada_en IS NULL) asignadas " +
            "FROM solicitud_ensamblaje_detalle d JOIN solicitud_ensamblaje_estado_actual e ON e.id_solicitud=d.id_solicitud " +
            "WHERE d.id_solicitud=? AND d.id_detalle=?",(rs,n) -> new Linea(rs.getLong(1),rs.getLong(2),
            rs.getString(3),rs.getString(4),rs.getString(5),rs.getInt(6),rs.getString(7),rs.getInt(8)),
            solicitudId,detalleId).stream().findFirst().orElse(null);
    }

    @Override
    public boolean bloquearMotocicleta(long id) {
        return !jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_motocicleta=? FOR UPDATE",
            (rs,n) -> rs.getLong(1),id).isEmpty();
    }

    @Override
    public Candidata candidata(long id) {
        return jdbc.query("SELECT m.id_motocicleta,m.chasis_completo,m.marca,m.modelo,m.color FROM motocicleta m " +
            "WHERE m.id_motocicleta=? AND NOT EXISTS(SELECT 1 FROM solicitud_ensamblaje_unidad a " +
            "WHERE a.id_motocicleta=m.id_motocicleta AND a.retirada_en IS NULL) " +
            "AND NOT EXISTS(SELECT 1 FROM venta_motocicleta v WHERE v.id_motocicleta=m.id_motocicleta) " +
            "AND coalesce((SELECT x.estado FROM disponibilidad_evento x WHERE x.id_motocicleta=m.id_motocicleta " +
            "ORDER BY x.id_evento DESC LIMIT 1),'HABILITADA') <> 'BLOQUEADA'",
            (rs,n) -> new Candidata(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)),
            id).stream().findFirst().orElse(null);
    }

    @Override
    public List<Candidata> buscarElegibles(String chasis,int limite) {
        return jdbc.query("SELECT m.id_motocicleta,m.chasis_completo,m.marca,m.modelo,m.color FROM motocicleta m " +
            "WHERE upper(m.chasis_completo) LIKE upper(?) AND NOT EXISTS(SELECT 1 FROM solicitud_ensamblaje_unidad a " +
            "WHERE a.id_motocicleta=m.id_motocicleta AND a.retirada_en IS NULL) " +
            "AND NOT EXISTS(SELECT 1 FROM venta_motocicleta v WHERE v.id_motocicleta=m.id_motocicleta) " +
            "AND coalesce((SELECT x.estado FROM disponibilidad_evento x WHERE x.id_motocicleta=m.id_motocicleta " +
            "ORDER BY x.id_evento DESC LIMIT 1),'HABILITADA') <> 'BLOQUEADA' " +
            "ORDER BY m.id_motocicleta LIMIT ?",
            (rs,n) -> new Candidata(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)),
            "%" + chasis.replace("\\","\\\\").replace("%","\\%").replace("_","\\_") + "%",limite);
    }

    @Override
    public Unidad asociar(long detalleId,long motocicletaId,String usuario) {
        Long id=jdbc.queryForObject("INSERT INTO solicitud_ensamblaje_unidad(id_detalle,id_motocicleta,id_asignador) " +
            "SELECT ?,?,id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO' RETURNING id_asociacion",
            Long.class,detalleId,motocicletaId,usuario);
        return jdbc.queryForObject("SELECT a.id_asociacion,a.id_motocicleta,m.chasis_completo,u.nombre_usuario,a.asociada_en " +
            "FROM solicitud_ensamblaje_unidad a JOIN motocicleta m ON m.id_motocicleta=a.id_motocicleta " +
            "JOIN usuario u ON u.id_usuario=a.id_asignador WHERE a.id_asociacion=?",this::mapear,id);
    }

    @Override
    public List<Unidad> asignadas(long detalleId) {
        return jdbc.query("SELECT a.id_asociacion,a.id_motocicleta,m.chasis_completo,u.nombre_usuario,a.asociada_en " +
            "FROM solicitud_ensamblaje_unidad a JOIN motocicleta m ON m.id_motocicleta=a.id_motocicleta " +
            "JOIN usuario u ON u.id_usuario=a.id_asignador WHERE a.id_detalle=? AND a.retirada_en IS NULL " +
            "ORDER BY a.id_asociacion",this::mapear,detalleId);
    }

    @Override
    public boolean retirar(long asociacionId,long detalleId,String usuario,String motivo) {
        return jdbc.update("UPDATE solicitud_ensamblaje_unidad SET retirada_en=CURRENT_TIMESTAMP," +
            "id_retirador=(SELECT id_usuario FROM usuario WHERE nombre_usuario=?),motivo_retiro=? " +
            "WHERE id_asociacion=? AND id_detalle=? AND retirada_en IS NULL",
            usuario,motivo,asociacionId,detalleId)>0;
    }

    private Unidad mapear(ResultSet rs,int row) throws SQLException {
        return new Unidad(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),
            rs.getObject(5,OffsetDateTime.class));
    }
}
