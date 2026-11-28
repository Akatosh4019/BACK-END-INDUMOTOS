package pe.indumotos.infrastructure.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SesionRenovableRepository {
    public record Renovacion(String token, String usuario, long version, Instant limite) {}
    private static final long INACTIVIDAD_MINUTOS = 30;
    private static final long DURACION_HORAS = 8;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public SesionRenovableRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public String crear(String usuario, long version, Instant ahora) {
        jdbc.update("DELETE FROM sesion_renovable WHERE limite_en < ?", Timestamp.from(ahora.minus(1, ChronoUnit.DAYS)));
        var id = UUID.randomUUID();
        var token = nuevoToken();
        var limite = ahora.plus(DURACION_HORAS, ChronoUnit.HOURS);
        jdbc.update("INSERT INTO sesion_renovable(id, familia, id_usuario, token_hash, sesion_version, expira_en, limite_en) " +
            "SELECT ?, ?, id_usuario, ?, ?, ?, ? FROM usuario WHERE nombre_usuario = ? AND estado = 'ACTIVO'",
            id, id, hash(token), version, Timestamp.from(ahora.plus(INACTIVIDAD_MINUTOS, ChronoUnit.MINUTES)),
            Timestamp.from(limite), usuario);
        return token;
    }

    @Transactional
    public Renovacion renovar(String anterior, Instant ahora) {
        if (anterior == null || anterior.isBlank()) return null;
        var filas = jdbc.query("SELECT s.id, s.familia, s.id_usuario, s.sesion_version, s.expira_en, s.limite_en, " +
                "s.usado_en, s.revocado_en, u.nombre_usuario, u.sesion_version, u.estado, r.estado " +
                "FROM sesion_renovable s JOIN usuario u ON u.id_usuario=s.id_usuario " +
                "JOIN rol r ON r.id_rol=u.id_rol WHERE s.token_hash=? FOR UPDATE OF s",
            (rs, row) -> new Object[] {rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getLong(3),
                rs.getLong(4), rs.getTimestamp(5).toInstant(), rs.getTimestamp(6).toInstant(),
                rs.getTimestamp(7), rs.getTimestamp(8), rs.getString(9), rs.getLong(10), rs.getString(11), rs.getString(12)},
            hash(anterior));
        if (filas.isEmpty()) return null;
        var f = filas.get(0);
        var familia = (UUID) f[1];
        if (f[6] != null) {
            jdbc.update("UPDATE sesion_renovable SET revocado_en=? WHERE familia=? AND revocado_en IS NULL",
                Timestamp.from(ahora), familia);
            return null;
        }
        if (f[7] != null || !ahora.isBefore((Instant) f[4]) || !ahora.isBefore((Instant) f[5])
            || !f[3].equals(f[9]) || !"ACTIVO".equals(f[10]) || !"ACTIVO".equals(f[11])) return null;
        var nuevo = nuevoToken();
        var vence = ahora.plus(INACTIVIDAD_MINUTOS, ChronoUnit.MINUTES);
        if (vence.isAfter((Instant) f[5])) vence = (Instant) f[5];
        jdbc.update("UPDATE sesion_renovable SET usado_en=? WHERE id=?", Timestamp.from(ahora), f[0]);
        jdbc.update("INSERT INTO sesion_renovable(id, familia, id_usuario, token_hash, sesion_version, expira_en, limite_en) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), familia, f[2], hash(nuevo), f[3],
            Timestamp.from(vence), Timestamp.from((Instant) f[5]));
        return new Renovacion(nuevo, (String) f[8], (long) f[3], (Instant) f[5]);
    }

    public void revocar(UUID familia, Instant ahora) {
        if (familia != null) jdbc.update("UPDATE sesion_renovable SET revocado_en=? WHERE familia=? AND revocado_en IS NULL",
            Timestamp.from(ahora), familia);
    }

    public UUID familia(String token) {
        return jdbc.queryForObject("SELECT familia FROM sesion_renovable WHERE token_hash=?", UUID.class, hash(token));
    }

    public boolean activa(UUID familia) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM sesion_renovable WHERE familia=? AND usado_en IS NULL " +
                "AND revocado_en IS NULL AND expira_en>CURRENT_TIMESTAMP AND limite_en>CURRENT_TIMESTAMP)",
            Boolean.class, familia));
    }

    private String nuevoToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 no disponible", ex); }
    }
}
