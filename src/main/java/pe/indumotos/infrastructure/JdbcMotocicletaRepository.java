package pe.indumotos.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.MotocicletaRepository;
import pe.indumotos.domain.Motocicleta;

@Repository
public class JdbcMotocicletaRepository implements MotocicletaRepository {
    private final JdbcTemplate jdbc;

    public JdbcMotocicletaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existeLlegada(long llegadaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM llegada WHERE id_llegada = ?)", Boolean.class, llegadaId));
    }

    @Override
    public Motocicleta guardar(long llegadaId, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua,
        String creadorUsuario) {
        return jdbc.queryForObject(
            "WITH nueva AS (INSERT INTO motocicleta " +
            "(id_llegada, marca, modelo, color, chasis_completo, numero_motor, dua, item_dua, id_creador) " +
            "SELECT ?, ?, ?, ?, ?, ?, ?, ?, u.id_usuario FROM usuario u " +
            "WHERE u.nombre_usuario = ? AND u.estado = 'ACTIVO' " +
            "RETURNING *) SELECT n.*, u.nombre_usuario FROM nueva n " +
            "JOIN usuario u ON u.id_usuario = n.id_creador",
            this::mapear, llegadaId, marca, modelo, color, chasisCompleto, numeroMotor,
            dua, itemDua, creadorUsuario);
    }

    @Override
    public Motocicleta buscarPorId(long id) {
        return jdbc.queryForObject(
            "SELECT m.*, u.nombre_usuario FROM motocicleta m " +
            "JOIN usuario u ON u.id_usuario = m.id_creador WHERE m.id_motocicleta = ?",
            this::mapear, id);
    }

    private Motocicleta mapear(ResultSet rs, int rowNum) throws SQLException {
        return new Motocicleta(rs.getLong("id_motocicleta"), rs.getLong("id_llegada"),
            rs.getString("marca"), rs.getString("modelo"), rs.getString("color"),
            rs.getString("chasis_completo"), rs.getString("numero_motor"),
            rs.getString("dua"), rs.getString("item_dua"), rs.getString("nombre_usuario"),
            rs.getObject("creado_en", OffsetDateTime.class));
    }
}
