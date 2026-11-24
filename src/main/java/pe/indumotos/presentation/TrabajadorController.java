package pe.indumotos.presentation;

import java.util.Locale;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/usuarios/{id}/datos")
public class TrabajadorController {
    public record Datos(String nombreCompleto, String nombreUsuario, String documento, String telefono) {}
    public record Cambio(@NotBlank @Size(max=180) String nombreCompleto,
        @NotBlank @Size(min=3,max=80) @Pattern(regexp="[A-Za-z0-9._-]+") String nombreUsuario,
        @Pattern(regexp="|[A-Za-z0-9-]{5,20}") String documento,
        @Pattern(regexp="|[+0-9 -]{6,30}") String telefono) {}

    private final JdbcTemplate jdbc;
    public TrabajadorController(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @GetMapping
    public Datos consultar(@PathVariable long id) {
        Datos datos=jdbc.query("SELECT nombre_completo,nombre_usuario,documento,telefono FROM usuario WHERE id_usuario=?",
            rs -> rs.next() ? new Datos(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)) : null,id);
        if(datos==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Trabajador no encontrado");
        return datos;
    }

    @PutMapping
    @Transactional
    public Datos actualizar(@PathVariable long id,@Valid @RequestBody Cambio cambio, Authentication actor) {
        jdbc.queryForObject("SELECT set_config('indumotos.actor', ?, true)", String.class, actor.getName());
        String anterior=jdbc.query("SELECT nombre_usuario FROM usuario WHERE id_usuario=? FOR UPDATE",
            rs -> rs.next() ? rs.getString(1) : null,id);
        if(anterior==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Trabajador no encontrado");
        String usuario=cambio.nombreUsuario().trim().toLowerCase(Locale.ROOT);
        String documento=cambio.documento()==null||cambio.documento().isBlank()?null:cambio.documento().trim().toUpperCase(Locale.ROOT);
        String telefono=cambio.telefono()==null||cambio.telefono().isBlank()?null:cambio.telefono().trim();
        jdbc.update("UPDATE usuario SET nombre_completo=?,nombre_usuario=?,documento=?,telefono=?,"+
            "actualizado_en=CURRENT_TIMESTAMP,sesion_version=sesion_version+? WHERE id_usuario=?",
            cambio.nombreCompleto().trim(),usuario,documento,telefono,anterior.equals(usuario)?0:1,id);
        return consultar(id);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String,String>> duplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","El usuario o documento ya está registrado"));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> error(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error",ex.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> invalido() {
        return ResponseEntity.badRequest().body(Map.of("error","Revisa los datos del trabajador"));
    }
}
