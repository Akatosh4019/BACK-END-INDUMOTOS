package pe.indumotos.application.catalogo;

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

    public record Opcion(String marca, String modelo, String color) {}

    public List<Opcion> listar() {
        return jdbc.query("SELECT c.marca,c.modelo,c.color FROM catalogo_motocicleta c " +
            "JOIN catalogo_modelo m ON lower(btrim(m.marca))=lower(btrim(c.marca)) " +
            "AND lower(btrim(m.modelo))=lower(btrim(c.modelo)) " +
            "WHERE m.activo ORDER BY c.marca,c.modelo,c.color",
            (rs,n) -> new Opcion(rs.getString(1),rs.getString(2),rs.getString(3)));
    }

    @Transactional
    public Opcion agregar(String marca, String modelo, String color, String usuario) {
        String m = marca.trim(), mod = modelo.trim(), c = color.trim();
        int insertadas = jdbc.update("INSERT INTO catalogo_motocicleta(marca,modelo,color,id_creador) " +
            "SELECT ?,?,?,id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO' " +
            "ON CONFLICT DO NOTHING", m,mod,c,usuario);
        if (insertadas == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La combinación ya existe en el catálogo");
        jdbc.update("INSERT INTO catalogo_modelo(marca,modelo) VALUES (?,?) ON CONFLICT DO NOTHING", m,mod);
        return new Opcion(m,mod,c);
    }
}
