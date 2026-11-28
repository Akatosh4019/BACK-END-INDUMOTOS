package pe.indumotos.presentation.seguridad;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/roles")
public class RolController {
    public record Rol(long id, String codigo, String nombre, String descripcion, String estado,
        int trabajadores, int permisos) {}
    public record Permiso(String codigo, String nombre, String descripcion, boolean asignado) {}
    public record Detalle(Rol rol, List<Permiso> permisos) {}
    public record CambioPermisos(List<String> codigos) {}
    public record DatosRol(String codigo, String nombre, String descripcion) {}
    public record EditarRol(String nombre, String descripcion, String estado) {}

    private static final Set<String> ROLES_BASE = Set.of("ADMIN", "ALMACEN", "INVENTARIOS",
        "COMERCIAL", "FACTURACION", "ENSAMBLAJE");

    private static final Set<String> RESERVADOS = Set.of("USUARIOS_CREAR", "USUARIOS_CONSULTAR",
        "USUARIOS_DESACTIVAR", "PERMISOS_ASIGNAR", "CATALOGO_GESTIONAR", "COMPANIA_CONSULTAR");
    private final JdbcTemplate jdbc;

    public RolController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Rol> listar(Authentication actor) {
        exigirAdministrador(actor);
        return listarSinAutorizacion();
    }

    @PostMapping
    @Transactional
    public Rol crear(@RequestBody DatosRol datos, Authentication actor) {
        exigirAdministrador(actor);
        if (datos == null || datos.codigo() == null ||
            !datos.codigo().matches("[A-Za-z][A-Za-z0-9_]{2,49}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código debe tener entre 3 y 50 letras, números o guiones bajos");
        String codigo = datos.codigo().toUpperCase(Locale.ROOT);
        String nombre = validarNombre(datos.nombre());
        String descripcion = validarDescripcion(datos.descripcion());
        registrarActor(actor);
        try {
            jdbc.update("INSERT INTO rol(codigo,nombre,descripcion) VALUES (?,?,?)", codigo, nombre, descripcion);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código del rol ya existe");
        }
        return buscar(codigo);
    }

    @PutMapping("/{codigo}")
    @Transactional
    public Rol editar(@PathVariable String codigo, @RequestBody EditarRol datos, Authentication actor) {
        exigirAdministrador(actor);
        Rol rol = buscar(codigo);
        if ("ADMIN".equals(rol.codigo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol ADMIN está protegido");
        if (datos == null || !("ACTIVO".equals(datos.estado()) || "INACTIVO".equals(datos.estado())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un estado válido");
        String nombre = validarNombre(datos.nombre());
        String descripcion = validarDescripcion(datos.descripcion());
        if ("INACTIVO".equals(datos.estado()) && rol.trabajadores() > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reasigna primero los trabajadores de este rol");
        registrarActor(actor);
        jdbc.update("UPDATE rol SET nombre=?,descripcion=?,estado=? WHERE id_rol=?",
            nombre, descripcion, datos.estado(), rol.id());
        return buscar(rol.codigo());
    }

    @DeleteMapping("/{codigo}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable String codigo, Authentication actor) {
        exigirAdministrador(actor);
        Rol rol = buscar(codigo);
        if (ROLES_BASE.contains(rol.codigo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los roles iniciales no se eliminan");
        if (rol.trabajadores() > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reasigna primero los trabajadores de este rol");
        registrarActor(actor);
        jdbc.update("DELETE FROM rol_permiso WHERE id_rol=?", rol.id());
        try {
            jdbc.update("DELETE FROM rol WHERE id_rol=?", rol.id());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol tiene registros relacionados y no puede eliminarse");
        }
        return ResponseEntity.noContent().build();
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre debe tener entre 1 y 100 caracteres");
        return nombre.trim();
    }

    private String validarDescripcion(String descripcion) {
        if (descripcion != null && descripcion.trim().length() > 300)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción supera 300 caracteres");
        return descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
    }

    private void registrarActor(Authentication actor) {
        jdbc.queryForObject("SELECT set_config('indumotos.actor', ?, true)", String.class, actor.getName());
    }

    @GetMapping("/{codigo}")
    public Detalle consultar(@PathVariable String codigo, Authentication actor) {
        exigirAdministrador(actor);
        Rol rol = buscar(codigo);
        var permisos = jdbc.query("SELECT p.codigo,p.nombre,p.descripcion," +
            "EXISTS(SELECT 1 FROM rol_permiso rp WHERE rp.id_rol=? AND rp.id_permiso=p.id_permiso) " +
            "FROM permiso p ORDER BY p.codigo", (rs,n) -> new Permiso(rs.getString(1),
            rs.getString(2),rs.getString(3), "ADMIN".equals(rol.codigo()) || rs.getBoolean(4)), rol.id());
        return new Detalle(rol, permisos);
    }

    @PutMapping("/{codigo}/permisos")
    @Transactional
    public Detalle actualizar(@PathVariable String codigo, @RequestBody CambioPermisos cambio,
        Authentication actor) {
        exigirAdministrador(actor);
        Rol rol = buscar(codigo);
        if (!"ACTIVO".equals(rol.estado()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede editar un rol inactivo");
        if (cambio == null || cambio.codigos() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica los permisos del rol");
        Set<String> elegidos = new HashSet<>(cambio.codigos());
        if (elegidos.size() != cambio.codigos().size() || elegidos.stream().anyMatch(c -> c == null || c.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay permisos repetidos o vacíos");
        Set<String> disponibles = new HashSet<>(jdbc.query("SELECT codigo FROM permiso", (rs,n) -> rs.getString(1)));
        if (!disponibles.containsAll(elegidos))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay permisos desconocidos");
        if ("ADMIN".equals(rol.codigo()) && !elegidos.containsAll(disponibles))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administración conserva todos los permisos");
        if (!"ADMIN".equals(rol.codigo()) && (!java.util.Collections.disjoint(elegidos, RESERVADOS)
            || elegidos.contains("VENTAS_REGISTRAR") && !"FACTURACION".equals(rol.codigo())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol incluye permisos reservados");
        jdbc.queryForObject("SELECT set_config('indumotos.actor', ?, true)", String.class, actor.getName());
        for (Permiso permiso : consultar(codigo, actor).permisos()) {
            if (permiso.asignado() && !elegidos.contains(permiso.codigo()))
                jdbc.update("DELETE FROM rol_permiso WHERE id_rol=? AND id_permiso=" +
                    "(SELECT id_permiso FROM permiso WHERE codigo=?)", rol.id(), permiso.codigo());
        }
        for (String permiso : elegidos) jdbc.update("INSERT INTO rol_permiso(id_rol,id_permiso) " +
            "SELECT ?,id_permiso FROM permiso WHERE codigo=? ON CONFLICT DO NOTHING", rol.id(), permiso);
        return consultar(codigo, actor);
    }

    private Rol buscar(String codigo) {
        return listarSinAutorizacion().stream().filter(r -> r.codigo().equalsIgnoreCase(codigo))
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
    }

    private List<Rol> listarSinAutorizacion() {
        return jdbc.query("SELECT r.id_rol,r.codigo,r.nombre,r.descripcion,r.estado," +
            "(SELECT count(*) FROM usuario u WHERE u.id_rol=r.id_rol)," +
            "(SELECT count(*) FROM rol_permiso rp WHERE rp.id_rol=r.id_rol) FROM rol r ORDER BY r.id_rol",
            (rs,n) -> new Rol(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getInt(6),rs.getInt(7)));
    }

    private void exigirAdministrador(Authentication actor) {
        if (actor == null || !Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM usuario u JOIN rol r ON r.id_rol=u.id_rol " +
            "WHERE u.nombre_usuario=? AND u.estado='ACTIVO' AND r.codigo='ADMIN' AND r.estado='ACTIVO')",
            Boolean.class, actor.getName())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo Administración puede editar roles");
    }
}
