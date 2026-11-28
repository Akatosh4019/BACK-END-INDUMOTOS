package pe.indumotos.presentation.solicitudes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/solicitudes/borrador")
public class BorradorSolicitudController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public BorradorSolicitudController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> consultar(Authentication actor) {
        var contenido = jdbc.query("SELECT contenido::text FROM borrador_solicitud WHERE usuario=?",
            (rs, n) -> rs.getString(1), actor.getName());
        if (contenido.isEmpty()) return Map.of();
        try { return Map.of("contenido", mapper.readTree(contenido.getFirst())); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR); }
    }

    @PutMapping
    public void guardar(@RequestBody JsonNode contenido, Authentication actor) {
        if (!contenido.isObject() || !contenido.path("lineas").isArray()
            || contenido.path("lineas").size() > 50 || contenido.toString().length() > 20000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Borrador inválido");
        jdbc.update("INSERT INTO borrador_solicitud(usuario,contenido) VALUES (?,?::jsonb) " +
            "ON CONFLICT(usuario) DO UPDATE SET contenido=EXCLUDED.contenido,actualizado_en=CURRENT_TIMESTAMP",
            actor.getName(), contenido.toString());
    }

    @DeleteMapping
    public void eliminar(Authentication actor) {
        jdbc.update("DELETE FROM borrador_solicitud WHERE usuario=?", actor.getName());
    }
}
