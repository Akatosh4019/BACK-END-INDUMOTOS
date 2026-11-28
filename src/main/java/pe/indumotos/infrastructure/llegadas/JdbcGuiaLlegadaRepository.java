package pe.indumotos.infrastructure.llegadas;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.llegadas.GuiaLlegadaRepository;

@Repository
public class JdbcGuiaLlegadaRepository implements GuiaLlegadaRepository {
    private final JdbcTemplate jdbc;

    public JdbcGuiaLlegadaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void guardar(long llegadaId, int orden, String nombre, String tipo, byte[] contenido) {
        jdbc.update("INSERT INTO llegada_guia_imagen " +
            "(id_llegada,orden,nombre_archivo,tipo_mime,tamano,contenido) VALUES (?,?,?,?,?,?)",
            llegadaId, orden, nombre, tipo, contenido.length, contenido);
    }

    @Override
    public List<Resumen> listar(long llegadaId) {
        return jdbc.query("SELECT id_imagen,orden,nombre_archivo,tipo_mime,tamano " +
            "FROM llegada_guia_imagen WHERE id_llegada=? ORDER BY orden",
            (rs, row) -> new Resumen(rs.getLong(1), rs.getInt(2), rs.getString(3),
                rs.getString(4), rs.getInt(5)), llegadaId);
    }

    @Override
    public Imagen buscar(long llegadaId, long imagenId) {
        return jdbc.queryForObject("SELECT id_imagen,id_llegada,orden,nombre_archivo,tipo_mime,contenido " +
            "FROM llegada_guia_imagen WHERE id_llegada=? AND id_imagen=?",
            (rs, row) -> new Imagen(rs.getLong(1), rs.getLong(2), rs.getInt(3),
                rs.getString(4), rs.getString(5), rs.getBytes(6)), llegadaId, imagenId);
    }
}
