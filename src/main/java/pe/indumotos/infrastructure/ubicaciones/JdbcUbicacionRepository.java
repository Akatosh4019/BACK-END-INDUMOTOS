package pe.indumotos.infrastructure.ubicaciones;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.ubicaciones.UbicacionRepository;

@Repository
public class JdbcUbicacionRepository implements UbicacionRepository {
    private final JdbcTemplate jdbc;

    public JdbcUbicacionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Sede> sedes() {
        return jdbc.query("SELECT id_ubicacion, codigo, nombre, tipo FROM ubicacion WHERE activo ORDER BY nombre",
            (rs, n) -> new Sede(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)));
    }

    @Override
    public Sede crearSede(String codigo, String nombre, String tipo) {
        return jdbc.queryForObject("INSERT INTO ubicacion(codigo, nombre, tipo) VALUES(?, ?, ?) " +
            "RETURNING id_ubicacion, codigo, nombre, tipo",
            (rs, n) -> new Sede(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)),
            codigo, nombre, tipo);
    }

    @Override
    public boolean existeMotocicleta(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM motocicleta WHERE id_motocicleta = ?)", Boolean.class, id));
    }

    @Override
    public Sede sede(long id) {
        return jdbc.query("SELECT id_ubicacion, codigo, nombre, tipo FROM ubicacion WHERE id_ubicacion = ? AND activo",
            (rs, n) -> new Sede(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), id)
            .stream().findFirst().orElse(null);
    }

    @Override
    public void bloquearMotocicleta(long id) {
        jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_motocicleta = ? FOR UPDATE",
            rs -> { }, id);
    }

    @Override
    public boolean enTransito(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM " +
            "envio_solicitud_motocicleta WHERE id_motocicleta=? AND recibido_en IS NULL)",
            Boolean.class,id));
    }

    @Override
    public boolean vendida(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM venta_motocicleta WHERE id_motocicleta=?)",
            Boolean.class,id));
    }

    @Override
    public boolean reservadaSinEntrega(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM " +
            "solicitud_ensamblaje_unidad a JOIN solicitud_ensamblaje_detalle d " +
            "ON d.id_detalle=a.id_detalle JOIN solicitud_ensamblaje_estado_actual e " +
            "ON e.id_solicitud=d.id_solicitud WHERE a.id_motocicleta=? AND a.retirada_en IS NULL " +
            "AND e.estado IN ('EN_PREPARACION','ATENDIDA') AND NOT EXISTS " +
            "(SELECT 1 FROM envio_solicitud_motocicleta t WHERE t.id_solicitud=d.id_solicitud " +
            "AND t.id_motocicleta=a.id_motocicleta AND t.recibido_en IS NOT NULL))",
            Boolean.class,id));
    }

    @Override
    public Movimiento actual(long motocicletaId) {
        return jdbc.query("SELECT m.id_movimiento, m.id_motocicleta, m.id_origen, m.id_destino, " +
                "d.nombre AS destino_nombre, m.tipo, u.nombre_usuario, m.ocurrido_en " +
                "FROM motocicleta_ubicacion_actual a JOIN movimiento_motocicleta m ON m.id_movimiento = a.id_movimiento " +
                "JOIN ubicacion d ON d.id_ubicacion = m.id_destino " +
                "JOIN usuario u ON u.id_usuario = m.id_registrador WHERE a.id_motocicleta = ?",
            this::mapear, motocicletaId).stream().findFirst().orElse(null);
    }

    @Override
    public List<Movimiento> historial(long motocicletaId) {
        return jdbc.query("SELECT m.id_movimiento, m.id_motocicleta, m.id_origen, m.id_destino, " +
                "d.nombre AS destino_nombre, m.tipo, u.nombre_usuario, m.ocurrido_en " +
                "FROM movimiento_motocicleta m JOIN ubicacion d ON d.id_ubicacion = m.id_destino " +
                "JOIN usuario u ON u.id_usuario = m.id_registrador WHERE m.id_motocicleta = ? " +
                "ORDER BY m.ocurrido_en, m.id_movimiento", this::mapear, motocicletaId);
    }

    @Override
    public Movimiento registrar(long motocicletaId, long destinoId, String usuario) {
        Long id = jdbc.queryForObject("INSERT INTO movimiento_motocicleta " +
            "(id_motocicleta, tipo, id_destino, id_registrador, ocurrido_en) " +
            "SELECT ?, 'UBICACION', ?, id_usuario, CURRENT_TIMESTAMP FROM usuario " +
            "WHERE nombre_usuario = ? AND estado = 'ACTIVO' RETURNING id_movimiento",
            Long.class, motocicletaId, destinoId, usuario);
        return jdbc.queryForObject("SELECT m.id_movimiento, m.id_motocicleta, m.id_origen, m.id_destino, " +
                "d.nombre AS destino_nombre, m.tipo, u.nombre_usuario, m.ocurrido_en " +
                "FROM movimiento_motocicleta m JOIN ubicacion d ON d.id_ubicacion = m.id_destino " +
                "JOIN usuario u ON u.id_usuario = m.id_registrador WHERE m.id_movimiento = ?",
            this::mapear, id);
    }

    @Override
    public Movimiento trasladar(long motocicletaId, long origenId, long destinoId, String usuario) {
        Long id=jdbc.queryForObject("INSERT INTO movimiento_motocicleta " +
            "(id_motocicleta,tipo,id_origen,id_destino,id_registrador,ocurrido_en) " +
            "SELECT ?, 'TRASLADO', ?, ?, id_usuario, CURRENT_TIMESTAMP FROM usuario " +
            "WHERE nombre_usuario=? AND estado='ACTIVO' RETURNING id_movimiento",
            Long.class,motocicletaId,origenId,destinoId,usuario);
        return jdbc.queryForObject("SELECT m.id_movimiento,m.id_motocicleta,m.id_origen,m.id_destino," +
            "d.nombre AS destino_nombre,m.tipo,u.nombre_usuario,m.ocurrido_en FROM movimiento_motocicleta m " +
            "JOIN ubicacion d ON d.id_ubicacion=m.id_destino " +
            "JOIN usuario u ON u.id_usuario=m.id_registrador WHERE m.id_movimiento=?",
            this::mapear,id);
    }

    private Movimiento mapear(ResultSet rs, int row) throws SQLException {
        Long origen = rs.getObject("id_origen", Long.class);
        return new Movimiento(rs.getLong("id_movimiento"), rs.getLong("id_motocicleta"), origen,
            rs.getLong("id_destino"), rs.getString("destino_nombre"), rs.getString("tipo"),
            rs.getString("nombre_usuario"), rs.getObject("ocurrido_en", OffsetDateTime.class));
    }
}
