package pe.indumotos.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogoMotocicletas {
    private final JdbcTemplate jdbc;

    public CatalogoMotocicletas(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Una combinación inactiva sigue ligada a sus motos, pero ya no se ofrece en nuevas solicitudes. */
    public record Opcion(long id, String marca, String modelo, String color, boolean activo) {}

    public List<Opcion> listar() {
        return jdbc.query("SELECT id_catalogo,marca,modelo,color,activo FROM catalogo_motocicleta " +
            "ORDER BY marca,modelo,color",
            (rs,n) -> new Opcion(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5)));
    }

    @Transactional
    public Opcion agregar(String marca, String modelo, String color, String usuario) {
        String m = marca.trim(), mod = modelo.trim(), c = color.trim();
        var creada = jdbc.query("INSERT INTO catalogo_motocicleta(marca,modelo,color,id_creador) " +
            "SELECT ?,?,?,id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO' " +
            "ON CONFLICT DO NOTHING RETURNING id_catalogo",
            (rs,n) -> rs.getLong(1), m,mod,c,usuario);
        if (creada.isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La combinación ya existe en el catálogo");
        return new Opcion(creada.get(0),m,mod,c,true);
    }

    @Transactional
    public Opcion cambiarEstado(long id, boolean activo, String usuario) {
        var actualizada = jdbc.query("UPDATE catalogo_motocicleta SET activo=?, actualizado_en=CURRENT_TIMESTAMP, " +
            "id_actualizador=(SELECT id_usuario FROM usuario WHERE nombre_usuario=?) WHERE id_catalogo=? " +
            "RETURNING id_catalogo,marca,modelo,color,activo",
            (rs,n) -> new Opcion(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5)),
            activo, usuario, id);
        if (actualizada.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La combinación no existe en el catálogo");
        return actualizada.get(0);
    }
}
