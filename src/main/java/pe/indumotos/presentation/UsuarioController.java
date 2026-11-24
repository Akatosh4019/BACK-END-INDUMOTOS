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
import org.springframework.transaction.annotation.Transactional;

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
        @NotBlank @Size(max=50) @Pattern(regexp="[A-Za-z_]+") String rolCodigo,
        Long tiendaId,
        @Pattern(regexp="|[A-Za-z0-9-]{5,20}") String documento,
        @Pattern(regexp="|[+0-9 -]{6,30}") String telefono) {}
    public record UsuarioCreado(Long id, String nombreCompleto, String nombreUsuario, String rolCodigo) {}
    public record UsuarioListado(Long id, String nombreCompleto, String nombreUsuario,
        String rolCodigo, String estado, Long tiendaId, String tiendaNombre) {}
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
            + "LEFT JOIN usuario_tienda ut ON ut.id_usuario=u.id_usuario "
            + "LEFT JOIN ubicacion ti ON ti.id_ubicacion=ut.id_tienda "
            + "WHERE (? = '' OR strpos(lower(u.nombre_usuario), lower(?)) > 0 "
            + "OR strpos(lower(u.nombre_completo), lower(?)) > 0) "
            + "AND (? = '' OR r.codigo = ?) AND (? = '' OR u.estado = ?)";
        Object[] parametros = {filtro, filtro, filtro, filtroRol, filtroRol, filtroEstado, filtroEstado};
        Long total = jdbc.queryForObject("SELECT count(*)" + condiciones, Long.class, parametros);
        List<UsuarioListado> cuentas = jdbc.query(
            "SELECT u.id_usuario,u.nombre_completo,u.nombre_usuario,r.codigo,u.estado,ut.id_tienda,ti.nombre"
                + condiciones + " ORDER BY u.nombre_usuario,u.id_usuario LIMIT ? OFFSET ?",
            (rs, row) -> new UsuarioListado(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5),rs.getObject(6,Long.class),rs.getString(7)),
            filtro, filtro, filtro, filtroRol, filtroRol, filtroEstado, filtroEstado,
            tamano, (long) pagina * tamano);
        return new PaginaUsuarios(cuentas, pagina, tamano, total == null ? 0 : total);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<UsuarioCreado> crear(@Valid @RequestBody CrearUsuario request,
        org.springframework.security.core.Authentication actor) {
        String rol = request.rolCodigo().trim().toUpperCase(Locale.ROOT);
        Long rolId = jdbc.query("SELECT id_rol FROM rol WHERE codigo = ? AND estado = 'ACTIVO'",
            rs -> rs.next() ? rs.getLong(1) : null, rol);
        if (rolId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no válido o inactivo");
        if (request.tiendaId() != null) {
            if (!rol.equals("COMERCIAL") && !rol.equals("FACTURACION"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sede solo corresponde a Comercial o Facturación");
            Boolean tiendaValida = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM ubicacion " +
                "WHERE id_ubicacion=? AND tipo='TIENDA' AND activo)", Boolean.class, request.tiendaId());
            if (!Boolean.TRUE.equals(tiendaValida))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tienda no válida");
        }
        String username = request.nombreUsuario().trim().toLowerCase(Locale.ROOT);
        Long id = jdbc.queryForObject("INSERT INTO usuario (id_rol, nombre_completo, nombre_usuario, clave_hash, documento, telefono) " +
            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_usuario", Long.class, rolId,
            request.nombreCompleto().trim(), username, encoder.encode(request.clave()),
            normalizarDocumento(request.documento()), normalizarTelefono(request.telefono()));
        if (request.tiendaId() != null)
            jdbc.update("INSERT INTO usuario_tienda(id_usuario,id_tienda,id_asignador) " +
                "SELECT ?,?,id_usuario FROM usuario WHERE nombre_usuario=?", id, request.tiendaId(),
                actor.getName());
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + id))
            .body(new UsuarioCreado(id, request.nombreCompleto().trim(), username, rol));
    }

    private static String normalizarDocumento(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizarTelefono(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<java.util.Map<String, String>> duplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(java.util.Map.of("error", "El usuario o documento ya está registrado"));
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
