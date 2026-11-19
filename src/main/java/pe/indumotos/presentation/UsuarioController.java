package pe.indumotos.presentation;

import java.net.URI;
import java.util.Locale;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public UsuarioController(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    public record CrearUsuario(@NotBlank @Size(max=180) String nombreCompleto,
        @NotBlank @Size(min=3, max=80) @Pattern(regexp="[A-Za-z0-9._-]+") String nombreUsuario,
        @NotBlank @Size(min=12, max=100) String clave,
        @NotBlank @Size(max=50) @Pattern(regexp="[A-Za-z_]+") String rolCodigo) {}
    public record UsuarioCreado(Long id, String nombreCompleto, String nombreUsuario, String rolCodigo) {}
    public record UsuarioListado(Long id, String nombreCompleto, String nombreUsuario,
        String rolCodigo, String estado) {}
    public record PaginaUsuarios(List<UsuarioListado> items, int pagina, int tamano, long totalElementos) {}

    @GetMapping
    public PaginaUsuarios listar(@RequestParam(defaultValue = "") String buscar,
        @RequestParam(defaultValue = "") String rol,
        @RequestParam(defaultValue = "") String estado,
        @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "10") int tamano) {
        if (pagina < 0 || tamano != 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página o tamaño no válidos");
        String filtro = buscar.trim();
        if (filtro.length() > 180)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Búsqueda demasiado larga");
        String filtroRol = rol.trim().toUpperCase(Locale.ROOT);
        String filtroEstado = estado.trim().toUpperCase(Locale.ROOT);
        if (filtroRol.length() > 50 || (!filtroEstado.isEmpty()
            && !filtroEstado.equals("ACTIVO") && !filtroEstado.equals("INACTIVO")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro no válido");
        String condiciones = " FROM usuario u JOIN rol r ON r.id_rol=u.id_rol "
            + "WHERE (? = '' OR strpos(lower(u.nombre_usuario), lower(?)) > 0 "
            + "OR strpos(lower(u.nombre_completo), lower(?)) > 0) "
            + "AND (? = '' OR r.codigo = ?) AND (? = '' OR u.estado = ?)";
        Object[] parametros = {filtro, filtro, filtro, filtroRol, filtroRol, filtroEstado, filtroEstado};
        Long total = jdbc.queryForObject("SELECT count(*)" + condiciones, Long.class, parametros);
        List<UsuarioListado> cuentas = jdbc.query(
            "SELECT u.id_usuario,u.nombre_completo,u.nombre_usuario,r.codigo,u.estado"
                + condiciones + " ORDER BY u.nombre_usuario,u.id_usuario LIMIT ? OFFSET ?",
            (rs, row) -> new UsuarioListado(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5)),
            filtro, filtro, filtro, filtroRol, filtroRol, filtroEstado, filtroEstado,
            tamano, (long) pagina * tamano);
        return new PaginaUsuarios(cuentas, pagina, tamano, total == null ? 0 : total);
    }

    @PostMapping
    public ResponseEntity<UsuarioCreado> crear(@Valid @RequestBody CrearUsuario request) {
        String rol = request.rolCodigo().trim().toUpperCase(Locale.ROOT);
        Long rolId = jdbc.query("SELECT id_rol FROM rol WHERE codigo = ? AND estado = 'ACTIVO'",
            rs -> rs.next() ? rs.getLong(1) : null, rol);
        if (rolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no válido o inactivo");
        String username = request.nombreUsuario().trim().toLowerCase(Locale.ROOT);
        Long id = jdbc.queryForObject("INSERT INTO usuario (id_rol, nombre_completo, nombre_usuario, clave_hash) " +
            "VALUES (?, ?, ?, ?) RETURNING id_usuario", Long.class, rolId,
            request.nombreCompleto().trim(), username, encoder.encode(request.clave()));
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + id))
            .body(new UsuarioCreado(id, request.nombreCompleto().trim(), username, rol));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<java.util.Map<String, String>> duplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(java.util.Map.of("error", "El nombre de usuario ya existe"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<java.util.Map<String, Object>> datosInvalidos(MethodArgumentNotValidException ex) {
        java.util.List<String> campos = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField()).distinct().sorted().toList();
        return ResponseEntity.badRequest().body(java.util.Map.of(
            "error", "Datos inválidos para crear la cuenta",
            "campos", campos));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String, String>> rolInvalido(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
            .body(java.util.Map.of("error", ex.getReason() == null ? "Solicitud inválida" : ex.getReason()));
    }
}
