package pe.indumotos.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.LlegadaRepository;
import pe.indumotos.domain.Llegada;

@Repository
public class JdbcLlegadaRepository implements LlegadaRepository {
    private final JdbcTemplate jdbc;

    public JdbcLlegadaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Llegada guardar(String numeroGuia, String receptorUsuario, int declarada,
        int recibida, String observacion) {
        return jdbc.queryForObject(
            "WITH nueva AS (INSERT INTO llegada " +
            "(numero_guia, id_receptor, cantidad_declarada, cantidad_recibida, observacion) " +
            "SELECT ?, u.id_usuario, ?, ?, ? FROM usuario u WHERE u.nombre_usuario = ? AND u.estado = 'ACTIVO' " +
            "RETURNING id_llegada, numero_guia, fecha_llegada, id_receptor, cantidad_declarada, cantidad_recibida, observacion) " +
            "SELECT n.*, u.nombre_usuario FROM nueva n JOIN usuario u ON u.id_usuario = n.id_receptor",
            this::mapear, numeroGuia, declarada, recibida, observacion, receptorUsuario);
    }

    @Override
    public Llegada buscarPorId(long id) {
        return jdbc.queryForObject(
            "SELECT l.*, u.nombre_usuario FROM llegada l JOIN usuario u ON u.id_usuario = l.id_receptor " +
            "WHERE l.id_llegada = ?", this::mapear, id);
    }

    @Override
    public List<Llegada> buscarPorGuia(String numeroGuia) {
        return jdbc.query(
            "SELECT l.*, u.nombre_usuario FROM llegada l JOIN usuario u ON u.id_usuario = l.id_receptor " +
            "WHERE lower(l.numero_guia) LIKE lower(?) ORDER BY l.fecha_llegada DESC, l.id_llegada DESC LIMIT 20",
            this::mapear, "%" + numeroGuia + "%");
    }

    @Override
    public List<Llegada> listar(String numeroGuia, int limite, long desplazamiento) {
        return jdbc.query("SELECT l.*, u.nombre_usuario FROM llegada l " +
            "JOIN usuario u ON u.id_usuario=l.id_receptor " +
            "WHERE (? = '' OR lower(l.numero_guia) LIKE lower(?)) " +
            "ORDER BY l.fecha_llegada DESC, l.id_llegada DESC LIMIT ? OFFSET ?",
            this::mapear, numeroGuia, "%" + numeroGuia + "%", limite, desplazamiento);
    }

    @Override
    public long contar(String numeroGuia) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM llegada " +
            "WHERE (? = '' OR lower(numero_guia) LIKE lower(?))", Long.class,
            numeroGuia, "%" + numeroGuia + "%");
        return total == null ? 0 : total;
    }

    private Llegada mapear(ResultSet rs, int rowNum) throws SQLException {
        return new Llegada(rs.getLong("id_llegada"), rs.getString("numero_guia"),
            rs.getObject("fecha_llegada", OffsetDateTime.class), rs.getLong("id_receptor"),
            rs.getString("nombre_usuario"), rs.getInt("cantidad_declarada"),
            rs.getInt("cantidad_recibida"), rs.getString("observacion"));
    }
}
