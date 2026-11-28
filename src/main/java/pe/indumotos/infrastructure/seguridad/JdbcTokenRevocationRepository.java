package pe.indumotos.infrastructure.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTokenRevocationRepository {
    private final JdbcTemplate jdbc;

    public JdbcTokenRevocationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void revocar(String token, Instant expiraEn) {
        jdbc.update("DELETE FROM token_revocado WHERE expira_en <= CURRENT_TIMESTAMP");
        jdbc.update("INSERT INTO token_revocado(token_hash, expira_en) VALUES (?, ?) " +
            "ON CONFLICT (token_hash) DO NOTHING", hash(token), Timestamp.from(expiraEn));
    }

    public boolean revocado(String token) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM token_revocado WHERE token_hash = ? " +
                "AND expira_en > CURRENT_TIMESTAMP)", Boolean.class, hash(token)));
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
